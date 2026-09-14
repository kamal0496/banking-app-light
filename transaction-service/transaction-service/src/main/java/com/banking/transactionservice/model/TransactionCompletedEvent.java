package com.banking.transactionservice.model;

import lombok.*;

import java.math.BigDecimal;


@Data
@Builder
@AllArgsConstructor
public class TransactionCompletedEvent {

    private final String transactionId;
    private final String senderAccountNumber;
    private final String receiverAccountNumber;
    private final BigDecimal amount;
    private final String description;
}
