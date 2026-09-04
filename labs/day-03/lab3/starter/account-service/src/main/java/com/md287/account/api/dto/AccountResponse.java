package com.md287.account.api.dto;

import com.md287.account.domain.Account;
import com.md287.account.domain.AccountStatus;
import com.md287.account.domain.AccountType;

import java.time.OffsetDateTime;

public record AccountResponse(
        String accountId,
        String customerId,
        AccountType accountType,
        AccountStatus status,
        String currency,
        String nickname,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        OffsetDateTime closedAt
) {
    public static AccountResponse from(Account account) {
        return new AccountResponse(
                account.getAccountId(),
                account.getCustomerId(),
                account.getAccountType(),
                account.getStatus(),
                account.getCurrency(),
                account.getNickname(),
                account.getCreatedAt(),
                account.getUpdatedAt(),
                account.getClosedAt()
        );
    }
}
