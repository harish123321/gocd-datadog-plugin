package com.pagerduty.go.notification.datadog;


import com.google.gson.GsonBuilder;
import com.thoughtworks.go.plugin.api.GoApplicationAccessor;
import com.thoughtworks.go.plugin.api.GoPlugin;
import com.thoughtworks.go.plugin.api.GoPluginIdentifier;
import com.thoughtworks.go.plugin.api.annotation.Extension;
import com.thoughtworks.go.plugin.api.logging.Logger;
import com.thoughtworks.go.plugin.api.request.GoPluginApiRequest;
import com.thoughtworks.go.plugin.api.response.GoPluginApiResponse;
import com.timgroup.statsd.Event;
import com.timgroup.statsd.NonBlockingStatsDClient;
import com.timgroup.statsd.StatsDClient;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import java.io.File;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.*;

@Extension
public class GoNotificationPlugin implements GoPlugin {
    private static final Logger LOGGER = Logger.getLoggerFor(GoNotificationPlugin.class);

    private static final String CONF_FILENAME = "datadog-notify.conf";

    private static String hostname;
    private static StatsDClient statsd;
    private static List<String> statesForHistograms;
    private static String histogramMetric;
    private static List<String> statesForEvents;

    public GoNotificationPlugin() {
        Config defaultConfig = null;
        Config config = null;

        defaultConfig = ConfigFactory.load(getClass().getClassLoader());

        String userHome = System.getProperty("user.home");
        File configFile = new File(userHome + File.separator + CONF_FILENAME);
        if (!configFile.exists()) {
            LOGGER.warn(String.format("The configuration file %s was not found, using defaults. The configuration file should be set up.", configFile));
            config = defaultConfig;
        } else {
            config = ConfigFactory.parseFile(configFile).withFallback(defaultConfig);
        }

        statesForHistograms = config.getStringList("datadog.create_histograms_for_states");
        histogramMetric = config.getString("datadog.histogram_metric");
        statesForEvents = config.getStringList("datadog.create_events_for_states");

        String prefix = config.getString("datadog.prefix");
        List<String> tagList = config.getStringList("datadog.tags");
        String[] tags = tagList.toArray(new String[tagList.size()]);
        String statsdHost = config.getString("statsd.host");
        int statsdPort = config.getInt("statsd.port");

        statsd = new NonBlockingStatsDClient(
                prefix,
                statsdHost,
                statsdPort,
                tags
        );

        try {
            hostname = InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            hostname = "";
        }
    }

    public void initializeGoApplicationAccessor(GoApplicationAccessor goApplicationAccessor) {
    }

    public GoPluginApiResponse handle(GoPluginApiRequest goPluginApiRequest) {
        if (goPluginApiRequest.requestName().equals("notifications-interested-in")) {
            return handleNotificationInterest();
        } else if (goPluginApiRequest.requestName().equals("stage-status")) {
            return handleStageNotification(goPluginApiRequest);
        }
        return null;
    }

    private GoPluginApiResponse handleNotificationInterest() {
        Map<String, Object> response = new HashMap<>();
        response.put("notifications", Collections.singletonList("stage-status"));
        return renderJSON(200, response);
    }

    private GoPluginApiResponse handleStageNotification(GoPluginApiRequest goPluginApiRequest) {
        Map<String, Object> response = new HashMap<>();
        int responseCode = 200;
        Long duration;

        List<String> messages = new ArrayList<>();

        try {
            response.put("status", "success");
            GoNotificationMessage message = new GsonBuilder().registerTypeAdapter(Date.class, new Iso8601DateAdapter()).create().fromJson(goPluginApiRequest.requestBody(), GoNotificationMessage.class);

            // Log duration of stage for certain states
            if (statesForHistograms.contains(message.getStageState())) {
                duration = message.getStageLastTransitionTime().getTime() - message.getStageCreateTime().getTime();
                statsd.recordHistogramValue(histogramMetric, duration, message.getPipelineName(), message.getStageName(), message.getStageResult());
            }

            // Log events for certain stage states
            if (statesForEvents.contains(message.getStageState())) {
                duration = message.getStageLastTransitionTime().getTime() - message.getStageCreateTime().getTime();

                Event.AlertType alertType = Event.AlertType.ERROR;
                switch (message.getStageState()) {
                    case "Passed":
                        alertType = Event.AlertType.SUCCESS;
                        break;
                    case "Failed":
                        alertType = Event.AlertType.ERROR;
                        break;
                    case "Cancelled":
                        alertType = Event.AlertType.WARNING;
                        break;
                }

                statsd.recordEvent(Event.builder()
                                .withTitle(String.format("GoCD %s %s on %s", message.fullyQualifiedJobName(), message.getStageResult(), hostname))
                                .withText(String.format("GoCD %s %s on %s", message.fullyQualifiedJobName(), message.getStageResult(), hostname))
                                .withDate(message.getStageLastTransitionTime().getTime())
                                .withAlertType(alertType)
                                .build(),
                        message.getPipelineName(),
                        message.getStageName(),
                        message.getStageResult()
                );

            }

        } catch (Exception e) {
            LOGGER.error("Error handling status message", e);
            responseCode = 500;
            response.put("status", "failure");
            if (!(e.getMessage().isEmpty())){
                messages.add(e.getMessage());
            }
        }

        if (!(messages.isEmpty())) {
            response.put("messages", messages);
        }

        return renderJSON(responseCode, response);
    }

    public GoPluginIdentifier pluginIdentifier() {
        return new GoPluginIdentifier("notification", Collections.singletonList("1.0"));
    }

    private GoPluginApiResponse renderJSON(final int responseCode, Object response) {
        final String json = response == null ? null : new GsonBuilder().create().toJson(response);
        return new GoPluginApiResponse() {
            @Override
            public int responseCode() {
                return responseCode;
            }

            @Override
            public Map<String, String> responseHeaders() {
                return null;
            }

            @Override
            public String responseBody() {
                return json;
            }
        };
    }
}
