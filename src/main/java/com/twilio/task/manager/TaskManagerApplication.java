package com.twilio.task.manager;

import io.dropwizard.core.Application;
import io.dropwizard.core.setup.Bootstrap;
import io.dropwizard.core.setup.Environment;
import io.dropwizard.configuration.EnvironmentVariableSubstitutor;
import io.dropwizard.configuration.SubstitutingSourceProvider;
import io.dropwizard.db.DataSourceFactory;
import io.dropwizard.jdbi3.JdbiFactory;
import io.dropwizard.migrations.MigrationsBundle;
import org.jdbi.v3.core.Jdbi;

import com.twilio.task.manager.db.TaskDAO;
import com.twilio.task.manager.db.UserDAO;
import com.twilio.task.manager.db.ProjectDAO;
import com.twilio.task.manager.kafka.KafkaConsumerManager;
import com.twilio.task.manager.kafka.KafkaEventPublisher;
import com.twilio.task.manager.resources.TasksResource;
import com.twilio.task.manager.resources.UsersResource;
import com.twilio.task.manager.resources.ProjectsResource;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;

import java.util.Properties;
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
        // Enable ${ENV_VAR:-default} substitution in config.yml
        bootstrap.setConfigurationSourceProvider(
            new SubstitutingSourceProvider(
                bootstrap.getConfigurationSourceProvider(),
                new EnvironmentVariableSubstitutor(false)
            )
        );

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

        // Build and register Kafka producer + consumer
        final KafkaConfiguration kafkaConfig = configuration.getKafka();
        final KafkaEventPublisher kafkaPublisher = new KafkaEventPublisher(buildProducerProps(kafkaConfig));
        final KafkaConsumerManager kafkaConsumer = new KafkaConsumerManager(
                buildConsumerProps(kafkaConfig), kafkaConfig.getConsumerTopics());

        environment.lifecycle().manage(kafkaPublisher);
        environment.lifecycle().manage(kafkaConsumer);

        // Register REST resources (producer injected for event publishing)
        environment.jersey().register(new TasksResource(taskDAO, kafkaPublisher, kafkaConfig.getTasksTopic()));
        environment.jersey().register(new UsersResource(userDAO, kafkaPublisher, kafkaConfig.getUsersTopic()));
        environment.jersey().register(new ProjectsResource(projectDAO, kafkaPublisher, kafkaConfig.getProjectsTopic()));

        // Configure Datadog metrics reporter via DogStatsD
        configureDatadog(configuration, environment);
    }

    private Properties buildProducerProps(KafkaConfiguration cfg) {
        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, cfg.getBootstrapServers());
        props.put(ProducerConfig.ACKS_CONFIG, "all");
        props.put(ProducerConfig.RETRIES_CONFIG, 3);
        props.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
        addSecurityProps(props, cfg);
        return props;
    }

    private Properties buildConsumerProps(KafkaConfiguration cfg) {
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, cfg.getBootstrapServers());
        props.put(ConsumerConfig.GROUP_ID_CONFIG, cfg.getConsumerGroupId());
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "true");
        addSecurityProps(props, cfg);
        return props;
    }

    private void addSecurityProps(Properties props, KafkaConfiguration cfg) {
        if (cfg.getSecurityProtocol() != null && !cfg.getSecurityProtocol().isBlank()) {
            props.put("security.protocol", cfg.getSecurityProtocol());
        }
        if (cfg.getSaslMechanism() != null && !cfg.getSaslMechanism().isBlank()) {
            props.put("sasl.mechanism", cfg.getSaslMechanism());
        }
        if (cfg.getSaslJaasConfig() != null && !cfg.getSaslJaasConfig().isBlank()) {
            props.put("sasl.jaas.config", cfg.getSaslJaasConfig());
        }
    }

    private void configureDatadog(TaskManagerConfiguration configuration, Environment environment) {
        DatadogConfiguration ddConfig = configuration.getDatadog();

        StatsDReporter reporter = StatsDReporter.forRegistry(environment.metrics())
                .withApiKey(ddConfig.getApiKey())
                .withSite(ddConfig.getSite())
                .withPrefix(ddConfig.getPrefix())
                .withTags(ddConfig.getTags())
                .build();

        reporter.start(10, TimeUnit.SECONDS);
    }

}
