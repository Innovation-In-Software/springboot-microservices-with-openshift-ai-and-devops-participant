package com.md287.account.api.dto;

import com.md287.account.domain.AccountType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateAccountRequest(
        @NotBlank
        @Pattern(regexp = "^CUST-[0-9]{4}$", message = "customerId must be a synthetic identifier such as CUST-0001")
        String customerId,

        @NotNull
        AccountType accountType,

        @NotBlank
        @Pattern(regexp = "^[A-Z]{3}$", message = "currency must be a 3-letter ISO code such as USD")
        String currency,

        @Size(max = 80)
        String nickname
) {
}
