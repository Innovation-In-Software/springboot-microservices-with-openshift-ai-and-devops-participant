package com.md287.risk.api.exception;

public class AssessmentNotFoundException extends RuntimeException {

    private final String transactionId;

    public AssessmentNotFoundException(String transactionId) {
        super("Assessment for transaction " + transactionId + " was not found");
        this.transactionId = transactionId;
    }

    public String getTransactionId() {
        return transactionId;
    }
}
