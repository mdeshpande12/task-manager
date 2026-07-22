package com.twilio.task.manager;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;

public class KafkaConfiguration {

    @JsonProperty
    private String bootstrapServers = "localhost:9092";

    @JsonProperty
    private String tasksTopic = "tasks";

    @JsonProperty
    private String usersTopic = "users";

    @JsonProperty
    private String projectsTopic = "projects";

    @JsonProperty
    private String consumerGroupId = "task-manager-group";

    @JsonProperty
    private List<String> consumerTopics = new ArrayList<>(List.of("tasks", "users", "projects"));

    @JsonProperty
    private String securityProtocol = "PLAINTEXT";

    /** Required when connecting to a remote cluster with SASL (e.g. SASL_SSL). */
    @JsonProperty
    private String saslMechanism = "";

    /** Full JAAS config string, e.g. for SCRAM-SHA-512 or PLAIN. */
    @JsonProperty
    private String saslJaasConfig = "";

    public String getBootstrapServers() {
        return bootstrapServers;
    }

    public String getTasksTopic() {
        return tasksTopic;
    }

    public String getUsersTopic() {
        return usersTopic;
    }

    public String getProjectsTopic() {
        return projectsTopic;
    }

    public String getConsumerGroupId() {
        return consumerGroupId;
    }

    public List<String> getConsumerTopics() {
        return consumerTopics;
    }

    public String getSecurityProtocol() {
        return securityProtocol;
    }

    public String getSaslMechanism() {
        return saslMechanism;
    }

    public String getSaslJaasConfig() {
        return saslJaasConfig;
    }
}
