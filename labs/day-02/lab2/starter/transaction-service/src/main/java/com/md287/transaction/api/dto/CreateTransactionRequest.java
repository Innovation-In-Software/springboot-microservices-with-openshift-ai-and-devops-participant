package com.md287.transaction.api.dto;

import com.md287.transaction.domain.TransactionType;

import java.math.BigDecimal;

public record CreateTransactionRequest(
        String accountId,
        BigDecimal amount,
        String currency,
        TransactionType type
) {
}
