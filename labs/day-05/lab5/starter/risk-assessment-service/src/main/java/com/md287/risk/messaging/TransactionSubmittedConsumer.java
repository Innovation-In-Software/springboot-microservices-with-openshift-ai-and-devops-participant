package com.md287.risk.messaging;

import com.md287.risk.config.CorrelationIdFilter;
import com.md287.risk.service.AssessmentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class TransactionSubmittedConsumer {

    private static final Logger log = LoggerFactory.getLogger(TransactionSubmittedConsumer.class);

    private final AssessmentService assessmentService;

    public TransactionSubmittedConsumer(AssessmentService assessmentService) {
        this.assessmentService = assessmentService;
    }

    @KafkaListener(topics = "${md287.kafka.submitted-topic}")
    public void consume(TransactionSubmittedEvent event) {
        if (event == null || event.eventId() == null || event.payload() == null
                || event.payload().transactionId() == null) {
            throw new IllegalArgumentException("Poison TransactionSubmitted event");
        }
        String previous = MDC.get(CorrelationIdFilter.MDC_KEY);
        MDC.put(CorrelationIdFilter.MDC_KEY, event.correlationId());
        try {
            assessmentService.assess(event);
        } finally {
            if (previous == null) {
                MDC.remove(CorrelationIdFilter.MDC_KEY);
            } else {
                MDC.put(CorrelationIdFilter.MDC_KEY, previous);
            }
        }
    }
}
