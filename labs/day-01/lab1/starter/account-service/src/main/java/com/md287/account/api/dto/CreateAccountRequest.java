package com.md287.account.api.dto;

import com.md287.account.domain.AccountType;

public record CreateAccountRequest(
        String customerId,
        AccountType accountType,
        String currency,
        String nickname
) {
}
