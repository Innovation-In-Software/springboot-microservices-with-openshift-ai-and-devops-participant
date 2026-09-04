package com.md287.transaction.api.exception;

public class TransactionNotFoundException extends RuntimeException {

    private final String transactionId;

    public TransactionNotFoundException(String transactionId) {
        super("Transaction was not found");
        this.transactionId = transactionId;
    }

    public String getTransactionId() {
        return transactionId;
    }
}
