package com.md287.risk.messaging;

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
    public record Payload(
            String transactionId,
            String accountId,
            BigDecimal amount,
            String currency,
            String type
    ) {
    }
}
