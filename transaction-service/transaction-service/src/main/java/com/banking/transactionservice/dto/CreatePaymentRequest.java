package com.banking.transactionservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreatePaymentRequest {
    private String accountNumber;
    private String receiverAccountNumber;
    private BigDecimal amount;
    private String description;
    private String transactionId;
}
