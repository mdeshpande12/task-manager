package com.twilio.task.manager.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.dropwizard.lifecycle.Managed;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;

/**
 * Dropwizard Managed wrapper around a KafkaProducer.
 * Inject this into resources to publish domain events.
 */
public class KafkaEventPublisher implements Managed {

    private static final Logger logger = LoggerFactory.getLogger(KafkaEventPublisher.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final KafkaProducer<String, String> producer;

    public KafkaEventPublisher(Properties producerProps) {
        this.producer = new KafkaProducer<>(producerProps, new StringSerializer(), new StringSerializer());
    }

    /**
     * Publish a domain event by serialising the payload object to JSON.
     *
     * @param topic  target Kafka topic
     * @param key    message key (e.g. entity ID as string)
     * @param payload object to serialise as the message value
     */
    public void publish(String topic, String key, Object payload) {
        String value;
        try {
            value = MAPPER.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            logger.error("Failed to serialise Kafka event payload for topic {}: {}", topic, e.getMessage());
            return;
        }
        producer.send(new ProducerRecord<>(topic, key, value), (metadata, exception) -> {
            if (exception != null) {
                logger.error("Failed to deliver event to topic {}: {}", topic, exception.getMessage());
            } else {
                logger.debug("Event published to topic={} partition={} offset={}",
                        topic, metadata.partition(), metadata.offset());
            }
        });
    }

    @Override
    public void start() {
        logger.info("Kafka producer started");
    }

    @Override
    public void stop() {
        logger.info("Flushing and closing Kafka producer");
        producer.flush();
        producer.close();
    }
}
