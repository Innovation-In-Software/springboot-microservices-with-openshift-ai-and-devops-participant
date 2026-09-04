package com.md287.transaction.messaging;

import com.md287.transaction.domain.TransactionType;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record TransactionSubmittedEvent(
        String eventType,
        String eventVersion,
        String eventId,
        String correlationId,
        OffsetDateTime occurredAt,
        Payload payload
) {
    public static final String TYPE = "TransactionSubmitted";
    public static final String VERSION = "1";

    public record Payload(
            String transactionId,
            String accountId,
            BigDecimal amount,
            String currency,
            TransactionType type
    ) {
    }
}
