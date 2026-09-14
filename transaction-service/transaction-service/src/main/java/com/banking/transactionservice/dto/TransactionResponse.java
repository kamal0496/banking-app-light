package com.banking.transactionservice.dto;

import com.banking.transactionservice.entity.TransactionStatus;
import com.banking.transactionservice.entity.TrasactionType;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class TransactionResponse {

    private String id;
    private String senderAccountNumber;
    private String receiverAccountNumber;
    private BigDecimal amount;
    private TrasactionType trasactionType;
    private TransactionStatus transactionStatus;
    private String referenceNumber;
    private String description;
    private String failureMessage;

    private LocalDateTime createdAt;

    private LocalDateTime completedAt;
}
