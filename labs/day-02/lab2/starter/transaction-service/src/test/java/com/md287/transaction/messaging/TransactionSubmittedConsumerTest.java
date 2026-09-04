package com.md287.transaction.messaging;

import com.md287.transaction.domain.ProcessedEvent;
import com.md287.transaction.domain.Transaction;
import com.md287.transaction.domain.TransactionStatus;
import com.md287.transaction.domain.TransactionType;
import com.md287.transaction.repository.ProcessedEventRepository;
import com.md287.transaction.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransactionSubmittedConsumerTest {

    @Mock
    private ProcessedEventRepository processedEventRepository;
    @Mock
    private TransactionRepository transactionRepository;

    private TransactionSubmittedConsumer consumer;

    @BeforeEach
    void setUp() {
        consumer = new TransactionSubmittedConsumer(processedEventRepository, transactionRepository);
    }

    @Test
    void consumeMarksTransactionSubmittedOnce() {
        Transaction transaction = sample();
        when(processedEventRepository.existsById("evt-1")).thenReturn(false);
        when(transactionRepository.findById("TXN-11111111")).thenReturn(Optional.of(transaction));

        consumer.consume(event("evt-1"));

        assertThat(transaction.getStatus()).isEqualTo(TransactionStatus.SUBMITTED);
        verify(processedEventRepository).save(any(ProcessedEvent.class));
    }

    @Test
    void consumeIgnoresDuplicateEventId() {
        when(processedEventRepository.existsById("evt-1")).thenReturn(true);

        consumer.consume(event("evt-1"));

        verify(transactionRepository, never()).findById(any());
        verify(processedEventRepository, never()).save(any());
    }

    @Test
    void consumeRejectsPoisonPayload() {
        assertThatThrownBy(() -> consumer.consume(
                new TransactionSubmittedEvent("TransactionSubmitted", "1", "evt-bad", "corr", OffsetDateTime.now(), null)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private Transaction sample() {
        return new Transaction(
                "TXN-11111111",
                "ACC-AABBCCDD",
                new BigDecimal("10.00"),
                "USD",
                TransactionType.DEBIT,
                TransactionStatus.RECEIVED,
                "corr",
                "evt-1",
                OffsetDateTime.parse("2026-01-15T10:00:00Z")
        );
    }

    private TransactionSubmittedEvent event(String eventId) {
        return new TransactionSubmittedEvent(
                "TransactionSubmitted",
                "1",
                eventId,
                "corr",
                OffsetDateTime.parse("2026-01-15T10:00:00Z"),
                new TransactionSubmittedEvent.Payload(
                        "TXN-11111111",
                        "ACC-AABBCCDD",
                        new BigDecimal("10.00"),
                        "USD",
                        TransactionType.DEBIT
                )
        );
    }
}
