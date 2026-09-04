package com.md287.account.api.exception;

public class InvalidAccountStateException extends RuntimeException {

    private final String accountId;
    private final String currentStatus;
    private final String action;

    public InvalidAccountStateException(String accountId, String currentStatus, String action) {
        super("Account cannot perform this status change from its current status");
        this.accountId = accountId;
        this.currentStatus = currentStatus;
        this.action = action;
    }

    public String getAccountId() {
        return accountId;
    }

    public String getCurrentStatus() {
        return currentStatus;
    }

    public String getAction() {
        return action;
    }
}
