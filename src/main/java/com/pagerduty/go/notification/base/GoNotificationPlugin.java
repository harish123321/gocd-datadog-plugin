package com.pagerduty.go.notification.base;


import com.google.gson.GsonBuilder;
import com.thoughtworks.go.plugin.api.GoApplicationAccessor;
import com.thoughtworks.go.plugin.api.GoPlugin;
import com.thoughtworks.go.plugin.api.GoPluginIdentifier;
import com.thoughtworks.go.plugin.api.annotation.Extension;
import com.thoughtworks.go.plugin.api.logging.Logger;
import com.thoughtworks.go.plugin.api.request.GoPluginApiRequest;
import com.thoughtworks.go.plugin.api.response.GoPluginApiResponse;

import java.util.*;

@Extension
public class GoNotificationPlugin implements GoPlugin {
    private static Logger LOGGER = Logger.getLoggerFor(GoNotificationPlugin.class);

    public void initializeGoApplicationAccessor(GoApplicationAccessor goApplicationAccessor) {
    }

    public GoPluginApiResponse handle(GoPluginApiRequest goPluginApiRequest) {
        if (goPluginApiRequest.requestName().equals("notifications-interested-in")) {
            return handleNotificationInterest();
        } else if (goPluginApiRequest.requestName().equals("stage-status")) {
            return handleStageNotification(goPluginApiRequest);
        }
        // TODO: Add handler for getConfiguration
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

        List<String> messages = new ArrayList<>();

        try {
            response.put("status", "success");
            GoNotificationMessage message = new GsonBuilder().registerTypeAdapter(Date.class, new Iso8601DateAdapter()).create().fromJson(goPluginApiRequest.requestBody(), GoNotificationMessage.class);

            Long duration = message.getStageLastTransitionTime().getTime() - message.getStageCreateTime().getTime();

            LOGGER.info(String.format("Pipeline: %s Stage: %s Status %s", message.getPipelineName(), message.getStageName(), message.getStageResult()));

            String logString = String.format("Start: %tD %tT End %tD %tT Duration %d", message.getStageCreateTime(), message.getStageCreateTime(), message.getStageLastTransitionTime(), message.getStageLastTransitionTime(), duration);

            LOGGER.info("Notification JSON: " + logString);

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
