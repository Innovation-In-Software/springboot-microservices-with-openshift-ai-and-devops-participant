package com.md287.account.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;

@Entity
@Table(name = "accounts")
public class Account {

    @Id
    @Column(name = "account_id", nullable = false, length = 36)
    private String accountId;

    @Column(name = "customer_id", nullable = false, length = 32)
    private String customerId;

    @Enumerated(EnumType.STRING)
    @Column(name = "account_type", nullable = false, length = 20)
    private AccountType accountType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private AccountStatus status;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Column(name = "nickname", length = 80)
    private String nickname;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Column(name = "closed_at")
    private OffsetDateTime closedAt;

    protected Account() {
        // JPA
    }

    public Account(
            String accountId,
            String customerId,
            AccountType accountType,
            AccountStatus status,
            String currency,
            String nickname,
            OffsetDateTime createdAt
    ) {
        this.accountId = accountId;
        this.customerId = customerId;
        this.accountType = accountType;
        this.status = status;
        this.currency = currency;
        this.nickname = nickname;
        this.createdAt = createdAt;
        this.updatedAt = createdAt;
    }

    public String getAccountId() {
        return accountId;
    }

    public String getCustomerId() {
        return customerId;
    }

    public AccountType getAccountType() {
        return accountType;
    }

    public AccountStatus getStatus() {
        return status;
    }

    public String getCurrency() {
        return currency;
    }

    public String getNickname() {
        return nickname;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public OffsetDateTime getClosedAt() {
        return closedAt;
    }

    public void updateNickname(String nickname, OffsetDateTime now) {
        this.nickname = nickname;
        this.updatedAt = now;
    }

    public void activate(OffsetDateTime now) {
        this.status = AccountStatus.ACTIVE;
        this.updatedAt = now;
    }

    public void freeze(OffsetDateTime now) {
        this.status = AccountStatus.FROZEN;
        this.updatedAt = now;
    }

    public void close(OffsetDateTime now) {
        this.status = AccountStatus.CLOSED;
        this.closedAt = now;
        this.updatedAt = now;
    }
}
