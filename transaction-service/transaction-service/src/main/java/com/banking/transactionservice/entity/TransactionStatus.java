package com.banking.transactionservice.entity;

public enum TransactionStatus {
    PENDING,
    PAYMENT_PENDING,
    PROCESSING,
    PENDING_VERIFICATION,
    COMPLETED,
    FAILED,
    FLAGGED
}
