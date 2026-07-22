package com.twilio.task.manager.kafka;

import io.dropwizard.lifecycle.Managed;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.errors.WakeupException;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Dropwizard Managed wrapper that runs a Kafka consumer poll loop on a
 * dedicated background thread.  Override or replace the record handler to
 * process incoming events.
 */
public class KafkaConsumerManager implements Managed {

    private static final Logger logger = LoggerFactory.getLogger(KafkaConsumerManager.class);

    private final KafkaConsumer<String, String> consumer;
    private final List<String> topics;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "kafka-consumer");
        t.setDaemon(true);
        return t;
    });

    public KafkaConsumerManager(Properties consumerProps, List<String> topics) {
        this.consumer = new KafkaConsumer<>(consumerProps, new StringDeserializer(), new StringDeserializer());
        this.topics = topics;
    }

    @Override
    public void start() {
        running.set(true);
        consumer.subscribe(topics);
        executor.submit(this::pollLoop);
        logger.info("Kafka consumer started, subscribed to topics: {}", topics);
    }

    private void pollLoop() {
        try {
            while (running.get()) {
                ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(500));
                records.forEach(record ->
                        logger.info("Consumed event: topic={} partition={} offset={} key={} value={}",
                                record.topic(), record.partition(), record.offset(),
                                record.key(), record.value())
                );
            }
        } catch (WakeupException e) {
            // expected on shutdown — ignore
        } catch (Exception e) {
            logger.error("Unexpected error in Kafka consumer poll loop", e);
        } finally {
            consumer.close();
        }
    }

    @Override
    public void stop() throws InterruptedException {
        running.set(false);
        consumer.wakeup();
        executor.shutdown();
        if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
            executor.shutdownNow();
        }
        logger.info("Kafka consumer stopped");
    }
}
