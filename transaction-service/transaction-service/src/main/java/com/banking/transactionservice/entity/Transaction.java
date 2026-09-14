package com.banking.transactionservice.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name="transactions")
@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    private String senderAccountNumber;
    private String receiverAccountNumber;
    private BigDecimal amount;
    @Enumerated(EnumType.STRING)
    private TrasactionType trasactionType;
    @Enumerated(EnumType.STRING)
    private TransactionStatus transactionStatus;
    private String referenceNumber;
    private String description;
    private String failureMessage;
    private String paymentReferenceId;

    @CreationTimestamp
    private LocalDateTime createdAt;

    private LocalDateTime completedAt;

}
