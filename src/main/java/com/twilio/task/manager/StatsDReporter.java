package com.twilio.task.manager;

import com.codahale.metrics.*;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.StringJoiner;
import java.util.concurrent.TimeUnit;

/**
 * A Dropwizard Metrics reporter that sends metrics to Datadog via the HTTP API.
 * No local agent required — posts directly to https://api.datadoghq.com/api/v2/series.
 */
public class StatsDReporter extends ScheduledReporter {

    private final String apiKey;
    private final String apiUrl;
    private final String prefix;
    private final List<String> tags;

    private StatsDReporter(MetricRegistry registry, String apiKey, String site, String prefix, List<String> tags) {
        super(registry, "datadog-http-reporter", MetricFilter.ALL, TimeUnit.SECONDS, TimeUnit.MILLISECONDS);
        this.apiKey = apiKey;
        this.apiUrl = "https://api." + site + "/api/v2/series";
        this.prefix = prefix;
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

        if (apiKey == null || apiKey.isEmpty()) {
            return; // No API key = silently skip
        }

        long now = System.currentTimeMillis() / 1000;
        List<String> series = new ArrayList<>();

        for (Map.Entry<String, Gauge> entry : gauges.entrySet()) {
            Object value = entry.getValue().getValue();
            if (value instanceof Number) {
                series.add(buildSeries(entry.getKey(), ((Number) value).doubleValue(), now, "gauge"));
            }
        }

        for (Map.Entry<String, Counter> entry : counters.entrySet()) {
            series.add(buildSeries(entry.getKey() + ".count", entry.getValue().getCount(), now, "count"));
        }

        for (Map.Entry<String, Meter> entry : meters.entrySet()) {
            Meter meter = entry.getValue();
            String name = entry.getKey();
            series.add(buildSeries(name + ".count", meter.getCount(), now, "count"));
            series.add(buildSeries(name + ".m1_rate", meter.getOneMinuteRate(), now, "gauge"));
        }

        for (Map.Entry<String, Timer> entry : timers.entrySet()) {
            Timer timer = entry.getValue();
            String name = entry.getKey();
            Snapshot snapshot = timer.getSnapshot();
            series.add(buildSeries(name + ".count", timer.getCount(), now, "count"));
            series.add(buildSeries(name + ".m1_rate", timer.getOneMinuteRate(), now, "gauge"));
            series.add(buildSeries(name + ".p50", snapshot.getMedian() / 1_000_000.0, now, "gauge"));
            series.add(buildSeries(name + ".p95", snapshot.get95thPercentile() / 1_000_000.0, now, "gauge"));
            series.add(buildSeries(name + ".p99", snapshot.get99thPercentile() / 1_000_000.0, now, "gauge"));
            series.add(buildSeries(name + ".max", snapshot.getMax() / 1_000_000.0, now, "gauge"));
        }

        for (Map.Entry<String, Histogram> entry : histograms.entrySet()) {
            Histogram histogram = entry.getValue();
            String name = entry.getKey();
            Snapshot snapshot = histogram.getSnapshot();
            series.add(buildSeries(name + ".count", histogram.getCount(), now, "count"));
            series.add(buildSeries(name + ".p50", snapshot.getMedian(), now, "gauge"));
            series.add(buildSeries(name + ".p95", snapshot.get95thPercentile(), now, "gauge"));
            series.add(buildSeries(name + ".p99", snapshot.get99thPercentile(), now, "gauge"));
        }

        if (!series.isEmpty()) {
            postMetrics(series);
        }
    }

    private String buildSeries(String metricName, double value, long timestamp, String type) {
        String fullName = prefix.isEmpty() ? metricName : prefix + "." + metricName;
        String tagsJson = buildTagsJson();
        return String.format(
            "{\"metric\":\"%s\",\"type\":3,\"points\":[{\"timestamp\":%d,\"value\":%f}],\"tags\":[%s]}",
            fullName, timestamp, value, tagsJson
        );
    }

    private String buildTagsJson() {
        StringJoiner joiner = new StringJoiner(",");
        for (String tag : tags) {
            joiner.add("\"" + tag + "\"");
        }
        return joiner.toString();
    }

    private void postMetrics(List<String> series) {
        try {
            String body = "{\"series\":[" + String.join(",", series) + "]}";
            URL url = new URL(apiUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("DD-API-KEY", apiKey);
            conn.setDoOutput(true);
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);

            try (OutputStream os = conn.getOutputStream()) {
                os.write(body.getBytes(StandardCharsets.UTF_8));
            }

            int responseCode = conn.getResponseCode();
            if (responseCode >= 400) {
                // Log but don't throw — metrics should never break the app
                System.err.println("Datadog API returned " + responseCode);
            }
            conn.disconnect();
        } catch (IOException e) {
            // Silently drop — metrics are best-effort
        }
    }

    public static class Builder {
        private final MetricRegistry registry;
        private String apiKey = "";
        private String site = "datadoghq.com";
        private String prefix = "";
        private List<String> tags = List.of();

        private Builder(MetricRegistry registry) {
            this.registry = registry;
        }

        public Builder withApiKey(String apiKey) {
            this.apiKey = apiKey;
            return this;
        }

        public Builder withSite(String site) {
            this.site = site;
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
            return new StatsDReporter(registry, apiKey, site, prefix, tags);
        }
    }
}
