package com.twilio.task.manager;

import io.dropwizard.core.Application;
import io.dropwizard.core.setup.Bootstrap;
import io.dropwizard.core.setup.Environment;
import io.dropwizard.db.DataSourceFactory;
import io.dropwizard.jdbi3.JdbiFactory;
import io.dropwizard.migrations.MigrationsBundle;
import org.jdbi.v3.core.Jdbi;

import com.twilio.task.manager.db.TaskDAO;
import com.twilio.task.manager.db.UserDAO;
import com.twilio.task.manager.db.ProjectDAO;
import com.twilio.task.manager.resources.TasksResource;
import com.twilio.task.manager.resources.UsersResource;
import com.twilio.task.manager.resources.ProjectsResource;

import java.util.concurrent.TimeUnit;

public class TaskManagerApplication extends Application<TaskManagerConfiguration> {

    public static void main(final String[] args) throws Exception {
        new TaskManagerApplication().run(args);
    }

    @Override
    public String getName() {
        return "TaskManager";
    }

    @Override
    public void initialize(final Bootstrap<TaskManagerConfiguration> bootstrap) {
        bootstrap.addBundle(new MigrationsBundle<TaskManagerConfiguration>() {
            @Override
            public DataSourceFactory getDataSourceFactory(TaskManagerConfiguration configuration) {
                return configuration.getDatabase();
            }
        });
    }

    @Override
    public void run(final TaskManagerConfiguration configuration,
                    final Environment environment) {
        // Create a managed JDBI instance connected to PostgreSQL
        final JdbiFactory factory = new JdbiFactory();
        final Jdbi jdbi = factory.build(environment, configuration.getDatabase(), "postgresql");

        // Create DAOs
        final TaskDAO taskDAO = jdbi.onDemand(TaskDAO.class);
        final UserDAO userDAO = jdbi.onDemand(UserDAO.class);
        final ProjectDAO projectDAO = jdbi.onDemand(ProjectDAO.class);

        // Register REST resources
        environment.jersey().register(new TasksResource(taskDAO));
        environment.jersey().register(new UsersResource(userDAO));
        environment.jersey().register(new ProjectsResource(projectDAO));

        // Configure Datadog metrics reporter via DogStatsD
        configureDatadog(configuration, environment);
    }

    private void configureDatadog(TaskManagerConfiguration configuration, Environment environment) {
        DatadogConfiguration ddConfig = configuration.getDatadog();

        StatsDReporter reporter = StatsDReporter.forRegistry(environment.metrics())
                .withHost(ddConfig.getHost())
                .withPort(ddConfig.getPort())
                .withPrefix(ddConfig.getPrefix())
                .withTags(ddConfig.getTags())
                .build();

        reporter.start(10, TimeUnit.SECONDS);
    }

}
