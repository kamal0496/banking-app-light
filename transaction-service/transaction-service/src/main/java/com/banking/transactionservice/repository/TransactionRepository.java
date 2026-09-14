package com.banking.transactionservice.repository;

import com.banking.transactionservice.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, String> {

    List<Transaction> findBySenderAccountNumberOrReceiverAccountNumberOrderByCreatedAtDesc(
            String senderAccountNumber,
            String receiverAccountNumber
    );

    Optional<Transaction> findByPaymentReferenceId(String paymentReferenceId);
}
