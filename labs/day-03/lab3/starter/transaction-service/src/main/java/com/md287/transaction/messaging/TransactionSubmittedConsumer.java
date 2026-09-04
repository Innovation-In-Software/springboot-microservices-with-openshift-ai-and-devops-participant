package com.md287.transaction.messaging;

import com.md287.transaction.config.CorrelationIdFilter;
import com.md287.transaction.domain.ProcessedEvent;
import com.md287.transaction.domain.Transaction;
import com.md287.transaction.repository.ProcessedEventRepository;
import com.md287.transaction.repository.TransactionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

@Component
public class TransactionSubmittedConsumer {

    private static final Logger log = LoggerFactory.getLogger(TransactionSubmittedConsumer.class);

    private final ProcessedEventRepository processedEventRepository;
    private final TransactionRepository transactionRepository;

    public TransactionSubmittedConsumer(
            ProcessedEventRepository processedEventRepository,
            TransactionRepository transactionRepository
    ) {
        this.processedEventRepository = processedEventRepository;
        this.transactionRepository = transactionRepository;
    }

    @KafkaListener(topics = "${md287.kafka.submitted-topic}")
    @Transactional
    public void consume(TransactionSubmittedEvent event) {
        if (event == null || event.eventId() == null || event.payload() == null
                || event.payload().transactionId() == null) {
            throw new IllegalArgumentException("Poison TransactionSubmitted event");
        }

        String previous = MDC.get(CorrelationIdFilter.MDC_KEY);
        MDC.put(CorrelationIdFilter.MDC_KEY, event.correlationId());
        try {
            if (processedEventRepository.existsById(event.eventId())) {
                log.info("Duplicate event ignored eventId={} transactionId={}",
                        event.eventId(), event.payload().transactionId());
                return;
            }

            Transaction transaction = transactionRepository.findById(event.payload().transactionId())
                    .orElseThrow(() -> new IllegalStateException(
                            "Unknown transactionId " + event.payload().transactionId()));

            transaction.markSubmitted(OffsetDateTime.now());
            processedEventRepository.save(new ProcessedEvent(
                    event.eventId(),
                    transaction.getTransactionId(),
                    OffsetDateTime.now()
            ));
            log.info("Consumed {} eventId={} transactionId={} status={}",
                    event.eventType(), event.eventId(), transaction.getTransactionId(), transaction.getStatus());
        } finally {
            if (previous == null) {
                MDC.remove(CorrelationIdFilter.MDC_KEY);
            } else {
                MDC.put(CorrelationIdFilter.MDC_KEY, previous);
            }
        }
    }
}
