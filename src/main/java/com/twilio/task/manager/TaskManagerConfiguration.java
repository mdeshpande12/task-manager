package com.twilio.task.manager;

import io.dropwizard.core.Configuration;
import io.dropwizard.db.DataSourceFactory;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public class TaskManagerConfiguration extends Configuration {

    @Valid
    @NotNull
    @JsonProperty("database")
    private DataSourceFactory database = new DataSourceFactory();

    @Valid
    @JsonProperty("datadog")
    private DatadogConfiguration datadog = new DatadogConfiguration();

    public DataSourceFactory getDatabase() {
        return database;
    }

    public void setDatabase(DataSourceFactory database) {
        this.database = database;
    }

    public DatadogConfiguration getDatadog() {
        return datadog;
    }

    public void setDatadog(DatadogConfiguration datadog) {
        this.datadog = datadog;
    }
}
