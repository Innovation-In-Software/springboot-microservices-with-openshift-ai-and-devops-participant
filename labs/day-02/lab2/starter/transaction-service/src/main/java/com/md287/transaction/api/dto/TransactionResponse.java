package com.md287.transaction.api.dto;

import com.md287.transaction.domain.Transaction;
import com.md287.transaction.domain.TransactionStatus;
import com.md287.transaction.domain.TransactionType;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record TransactionResponse(
        String transactionId,
        String accountId,
        BigDecimal amount,
        String currency,
        TransactionType type,
        TransactionStatus status,
        String correlationId,
        String eventId,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
    public static TransactionResponse from(Transaction transaction) {
        return new TransactionResponse(
                transaction.getTransactionId(),
                transaction.getAccountId(),
                transaction.getAmount(),
                transaction.getCurrency(),
                transaction.getType(),
                transaction.getStatus(),
                transaction.getCorrelationId(),
                transaction.getEventId(),
                transaction.getCreatedAt(),
                transaction.getUpdatedAt()
        );
    }
}
