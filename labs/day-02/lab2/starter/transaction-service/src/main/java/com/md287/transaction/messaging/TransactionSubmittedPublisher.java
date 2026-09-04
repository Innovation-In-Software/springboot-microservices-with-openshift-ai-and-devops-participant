package com.md287.transaction.messaging;

import com.md287.transaction.config.Md287Properties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

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
        // TODO Lab 2 Step 6 — send the event to the pre-created Kafka topic.
        throw new UnsupportedOperationException("TODO: implement publish()");
    }
}
