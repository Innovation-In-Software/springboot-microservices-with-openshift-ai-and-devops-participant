package com.md287.risk.api.exception;

public class ReviewNotAllowedException extends RuntimeException {

    private final String transactionId;

    public ReviewNotAllowedException(String transactionId, String message) {
        super(message);
        this.transactionId = transactionId;
    }

    public String getTransactionId() {
        return transactionId;
    }
}
