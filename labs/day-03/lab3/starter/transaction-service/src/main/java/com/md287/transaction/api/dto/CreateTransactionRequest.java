package com.md287.transaction.api.dto;

import com.md287.transaction.domain.TransactionType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.math.BigDecimal;

public record CreateTransactionRequest(
        @NotBlank
        @Pattern(regexp = "^ACC-[0-9A-F]{8}$", message = "accountId must be a synthetic identifier such as ACC-AABBCCDD")
        String accountId,

        @NotNull
        @DecimalMin(value = "0.01", message = "amount must be at least 0.01")
        BigDecimal amount,

        @NotBlank
        @Pattern(regexp = "^[A-Z]{3}$", message = "currency must be a 3-letter ISO code such as USD")
        String currency,

        @NotNull
        TransactionType type
) {
}
