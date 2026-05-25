package com.twilio.task.manager;

import com.codahale.metrics.*;
import com.timgroup.statsd.NonBlockingStatsDClientBuilder;
import com.timgroup.statsd.StatsDClient;

import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.concurrent.TimeUnit;

/**
 * A Dropwizard Metrics reporter that sends metrics to Datadog via DogStatsD (UDP).
 */
public class StatsDReporter extends ScheduledReporter {

    private final StatsDClient client;
    private final String[] tags;

    private StatsDReporter(MetricRegistry registry, StatsDClient client, String[] tags) {
        super(registry, "statsd-reporter", MetricFilter.ALL, TimeUnit.SECONDS, TimeUnit.MILLISECONDS);
        this.client = client;
        this.tags = tags;
    }

    public static Builder forRegistry(MetricRegistry registry) {
        return new Builder(registry);
    }

    @Override
    public void report(SortedMap<String, Gauge> gauges,
                       SortedMap<String, Counter> counters,
                       SortedMap<String, Histogram> histograms,
                       SortedMap<String, Meter> meters,
                       SortedMap<String, Timer> timers) {

        for (Map.Entry<String, Gauge> entry : gauges.entrySet()) {
            Object value = entry.getValue().getValue();
            if (value instanceof Number) {
                client.gauge(entry.getKey(), ((Number) value).doubleValue(), tags);
            }
        }

        for (Map.Entry<String, Counter> entry : counters.entrySet()) {
            client.count(entry.getKey(), entry.getValue().getCount(), tags);
        }

        for (Map.Entry<String, Meter> entry : meters.entrySet()) {
            Meter meter = entry.getValue();
            String name = entry.getKey();
            client.gauge(name + ".count", meter.getCount(), tags);
            client.gauge(name + ".m1_rate", meter.getOneMinuteRate(), tags);
            client.gauge(name + ".m5_rate", meter.getFiveMinuteRate(), tags);
            client.gauge(name + ".mean_rate", meter.getMeanRate(), tags);
        }

        for (Map.Entry<String, Histogram> entry : histograms.entrySet()) {
            Histogram histogram = entry.getValue();
            String name = entry.getKey();
            Snapshot snapshot = histogram.getSnapshot();
            client.gauge(name + ".count", histogram.getCount(), tags);
            client.gauge(name + ".p50", snapshot.getMedian(), tags);
            client.gauge(name + ".p75", snapshot.get75thPercentile(), tags);
            client.gauge(name + ".p95", snapshot.get95thPercentile(), tags);
            client.gauge(name + ".p99", snapshot.get99thPercentile(), tags);
            client.gauge(name + ".max", snapshot.getMax(), tags);
            client.gauge(name + ".min", snapshot.getMin(), tags);
        }

        for (Map.Entry<String, Timer> entry : timers.entrySet()) {
            Timer timer = entry.getValue();
            String name = entry.getKey();
            Snapshot snapshot = timer.getSnapshot();
            client.gauge(name + ".count", timer.getCount(), tags);
            client.gauge(name + ".m1_rate", timer.getOneMinuteRate(), tags);
            client.gauge(name + ".p50", snapshot.getMedian() / 1_000_000.0, tags);  // ns → ms
            client.gauge(name + ".p75", snapshot.get75thPercentile() / 1_000_000.0, tags);
            client.gauge(name + ".p95", snapshot.get95thPercentile() / 1_000_000.0, tags);
            client.gauge(name + ".p99", snapshot.get99thPercentile() / 1_000_000.0, tags);
            client.gauge(name + ".max", snapshot.getMax() / 1_000_000.0, tags);
            client.gauge(name + ".min", snapshot.getMin() / 1_000_000.0, tags);
        }
    }

    @Override
    public void stop() {
        super.stop();
        client.close();
    }

    public static class Builder {
        private final MetricRegistry registry;
        private String host = "localhost";
        private int port = 8125;
        private String prefix = "";
        private List<String> tags = List.of();

        private Builder(MetricRegistry registry) {
            this.registry = registry;
        }

        public Builder withHost(String host) {
            this.host = host;
            return this;
        }

        public Builder withPort(int port) {
            this.port = port;
            return this;
        }

        public Builder withPrefix(String prefix) {
            this.prefix = prefix;
            return this;
        }

        public Builder withTags(List<String> tags) {
            this.tags = tags;
            return this;
        }

        public StatsDReporter build() {
            StatsDClient client = new NonBlockingStatsDClientBuilder()
                    .hostname(host)
                    .port(port)
                    .prefix(prefix)
                    .build();

            return new StatsDReporter(registry, client, tags.toArray(new String[0]));
        }
    }
}
