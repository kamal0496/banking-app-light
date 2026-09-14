package com.banking.paymentservice.dto.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

public class PaymentOrderResponse {

    private String paymentId;
    private String razorpayOrderId;
    private BigDecimal amount;
    private String currency;
    private String status;
    private String razorpayKeyId;
    private String transactionId;
    private String receiverAccountNumber;

    public PaymentOrderResponse() {
    }

    public PaymentOrderResponse(String paymentId, String razorpayOrderId, BigDecimal amount, String currency,
                               String status, String razorpayKeyId, String transactionId, String receiverAccountNumber) {
        this.paymentId = paymentId;
        this.razorpayOrderId = razorpayOrderId;
        this.amount = amount;
        this.currency = currency;
        this.status = status;
        this.razorpayKeyId = razorpayKeyId;
        this.transactionId = transactionId;
        this.receiverAccountNumber = receiverAccountNumber;
    }

    public String getPaymentId() { return paymentId; }
    public void setPaymentId(String paymentId) { this.paymentId = paymentId; }
    public String getRazorpayOrderId() { return razorpayOrderId; }
    public void setRazorpayOrderId(String razorpayOrderId) { this.razorpayOrderId = razorpayOrderId; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getRazorpayKeyId() { return razorpayKeyId; }
    public void setRazorpayKeyId(String razorpayKeyId) { this.razorpayKeyId = razorpayKeyId; }
    public String getTransactionId() { return transactionId; }
    public void setTransactionId(String transactionId) { this.transactionId = transactionId; }
    public String getReceiverAccountNumber() { return receiverAccountNumber; }
    public void setReceiverAccountNumber(String receiverAccountNumber) { this.receiverAccountNumber = receiverAccountNumber; }
}
