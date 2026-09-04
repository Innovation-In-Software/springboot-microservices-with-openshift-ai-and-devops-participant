package com.md287.transaction.messaging;

import com.md287.transaction.config.Md287Properties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Component
public class TransactionSubmittedPublisher {

    private static final Logger log = LoggerFactory.getLogger(TransactionSubmittedPublisher.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final String topic;

    public TransactionSubmittedPublisher(
            KafkaTemplate<String, Object> kafkaTemplate,
            Md287Properties properties
    ) {
        this.kafkaTemplate = kafkaTemplate;
        this.topic = properties.kafka().submittedTopic();
    }

    public void publish(TransactionSubmittedEvent event) {
        try {
            kafkaTemplate.send(topic, event.eventId(), event).get(5, TimeUnit.SECONDS);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while publishing TransactionSubmitted", ex);
        } catch (ExecutionException | TimeoutException ex) {
            throw new IllegalStateException("Failed to publish TransactionSubmitted", ex);
        }
        log.info("Published {} eventId={} transactionId={}",
                event.eventType(), event.eventId(), event.payload().transactionId());
    }
}
