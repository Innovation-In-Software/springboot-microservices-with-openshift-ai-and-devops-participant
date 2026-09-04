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
        // TODO Lab 2 Step 7 — ignore duplicates, mark SUBMITTED, reject poison payloads.
        throw new UnsupportedOperationException("TODO: implement consume()");
    }
}
