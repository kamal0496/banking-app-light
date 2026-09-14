package com.banking.paymentservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

public class CreatePaymentRequest {

    private String accountNumber;
    private String receiverAccountNumber;
    private BigDecimal amount;
    private String description;
    private String transactionId;

    public CreatePaymentRequest() {
    }

    public CreatePaymentRequest(String accountNumber, String receiverAccountNumber, BigDecimal amount,
                               String description, String transactionId) {
        this.accountNumber = accountNumber;
        this.receiverAccountNumber = receiverAccountNumber;
        this.amount = amount;
        this.description = description;
        this.transactionId = transactionId;
    }

    public String getAccountNumber() { return accountNumber; }
    public void setAccountNumber(String accountNumber) { this.accountNumber = accountNumber; }
    public String getReceiverAccountNumber() { return receiverAccountNumber; }
    public void setReceiverAccountNumber(String receiverAccountNumber) { this.receiverAccountNumber = receiverAccountNumber; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getTransactionId() { return transactionId; }
    public void setTransactionId(String transactionId) { this.transactionId = transactionId; }
}
