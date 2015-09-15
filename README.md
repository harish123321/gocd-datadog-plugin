# GoCD DataDog Notifier Plugin #

The GoCD DataDog Notifier Plugin will send stage run duration statistics and stage run events to DataDog. 

## Requirements ##

The plugin does not communicate directly with DataDog, it uses a DataDog agent to communicate. A DataDog agent must be available (best if this is on localhost).

## Configuration ##

The configuration file should be placed in the GoCD user's home directory.
It uses the [HOCON](https://github.com/typesafehub/config/blob/master/HOCON.md) format. Any or all of the configuration values may be left out and they will be defaulted to the values shown in the sample file below. 

Example file contents:

    datadog {
      # Prefix that will be prepended to all metric and event names
      prefix = gocd
      # Tags that will be added to all metrics and events eg [gocd, production]
      tags = [gocd]
      # Create build duration histogram for the following stage states (May be any of Passed, Failed, Cancelled)
      create_histograms_for_states = [Passed, Failed]
      # Build duration histogram metric name
      histogram_metric = build.duration
      # Create events for stage state (May be any of Passed, Failed, Cancelled)
      create_events_for_states = [Passed, Failed, Cancelled]
    }
    statsd {
      # Datadog agent / statsd host
      host = localhost
      # Datadog agent / statsd port
      port = 8125
    }

## License ##

This plugin was based on the GoCD Slack Notification Plugin
https://github.com/ashwanthkumar/gocd-slack-build-notifier