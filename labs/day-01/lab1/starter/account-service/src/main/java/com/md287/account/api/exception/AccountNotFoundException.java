package com.md287.account.api.exception;

public class AccountNotFoundException extends RuntimeException {

    private final String accountId;

    public AccountNotFoundException(String accountId) {
        super("Account was not found");
        this.accountId = accountId;
    }

    public String getAccountId() {
        return accountId;
    }
}
