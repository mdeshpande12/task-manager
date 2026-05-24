package com.twilio.task.manager;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;

public class DatadogConfiguration {

    @JsonProperty
    private String apiKey = "";

    @JsonProperty
    private String host = "localhost";

    @JsonProperty
    private int port = 8125;

    @JsonProperty
    private String prefix = "task-manager";

    @JsonProperty
    private List<String> tags = new ArrayList<>();

    public String getApiKey() {
        return apiKey;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public String getPrefix() {
        return prefix;
    }

    public List<String> getTags() {
        return tags;
    }
}
