package com.md287.transaction.api.exception;

public class AccountNotEligibleException extends RuntimeException {

    private final String accountId;
    private final String accountStatus;

    public AccountNotEligibleException(String accountId, String accountStatus) {
        super("Account is not eligible for transactions");
        this.accountId = accountId;
        this.accountStatus = accountStatus;
    }

    public String getAccountId() {
        return accountId;
    }

    public String getAccountStatus() {
        return accountStatus;
    }
}
