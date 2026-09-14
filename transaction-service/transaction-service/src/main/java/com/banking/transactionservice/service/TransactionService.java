package com.banking.transactionservice.service;

import com.banking.transactionservice.client.AccountServiceClient;
import com.banking.transactionservice.client.PaymentServiceClient;
import com.banking.transactionservice.dto.CreatePaymentRequest;
import com.banking.transactionservice.dto.PaymentOrderResponse;
import com.banking.transactionservice.dto.TransactionResponse;
import com.banking.transactionservice.dto.TransferRequest;
import com.banking.transactionservice.entity.Transaction;
import com.banking.transactionservice.entity.TransactionStatus;
import com.banking.transactionservice.entity.TrasactionType;
import com.banking.transactionservice.event.TransactionInitiatedEvent;
import com.banking.transactionservice.model.TransactionCompletedEvent;
import com.banking.transactionservice.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionService {

    private final TransactionRepository repo;
    private final AccountServiceClient accountServiceClient;
    private final PaymentServiceClient paymentServiceClient;

    private final RedisTemplate<String, Object> redisTemplate;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    private static final String TRANSACTION_INITIATED_TOPIC = "transaction.initiated";
    private static final String TRANSACTION_COMPLETED_TOPIC = "transaction.completed";
    private static final String TRANSACTION_REFUNDED_TOPIC = "transaction.refunded";
    private static final String FRAUD_DETECTED_TOPIC = "fraud.detected";

    public TransactionResponse transfer(TransferRequest request){

        /*
        * Initiate Transfer
        * Deduct balance from account service
        * save transaction as processing
        * publish event to kafka for fraud check
        * return
        * */
        accountServiceClient.deductBalance(
                request.getSenderAccountNumber(),
                request.getAmount()
        );

        Transaction transaction = Transaction.builder()
                .senderAccountNumber(request.getSenderAccountNumber())
                .receiverAccountNumber(request.getReceiverAccountNumber())
                .amount(request.getAmount())
                .description(request.getDescription())
                .trasactionType(TrasactionType.TRANSFER)
                .transactionStatus(TransactionStatus.PROCESSING)
                .referenceNumber(UUID.randomUUID().toString()).build();

        Transaction savedTransaction = repo.save(transaction);
        log.info("Transaction saved as processing for {} ", savedTransaction.getId());


        TransactionInitiatedEvent event = TransactionInitiatedEvent.builder()
                .transactionId(savedTransaction.getId())
                .senderAccountNumber(savedTransaction.getSenderAccountNumber())
                .receiverAccountNumber(savedTransaction.getReceiverAccountNumber())
                .amount(savedTransaction.getAmount())
                .description(savedTransaction.getDescription())
                .build();
        log.info("TransactionInitiatedEvent published for transactionId: {}", savedTransaction.getId());

        kafkaTemplate.send(TRANSACTION_INITIATED_TOPIC, savedTransaction.getId(), event);

        return mapToTransactionResponse(transaction);
    }

    public TransactionResponse transferWithPayment(TransferRequest request) {
        Transaction transaction = Transaction.builder()
                .senderAccountNumber(request.getSenderAccountNumber())
                .receiverAccountNumber(request.getReceiverAccountNumber())
                .amount(request.getAmount())
                .description(request.getDescription())
                .trasactionType(TrasactionType.TRANSFER)
                .transactionStatus(TransactionStatus.PAYMENT_PENDING)
                .referenceNumber(UUID.randomUUID().toString())
                .build();

        Transaction savedTransaction = repo.save(transaction);
        log.info("Transaction {} created and waiting for payment confirmation", savedTransaction.getId());

        CreatePaymentRequest paymentRequest = new CreatePaymentRequest(
                request.getSenderAccountNumber(),
                request.getReceiverAccountNumber(),
                request.getAmount(),
                request.getDescription(),
                savedTransaction.getId()
        );

        PaymentOrderResponse paymentOrder = paymentServiceClient.createPaymentOrder(paymentRequest);
        savedTransaction.setPaymentReferenceId(paymentOrder.getRazorpayOrderId());
        savedTransaction.setDescription(request.getDescription());
        repo.save(savedTransaction);

        log.info("Payment order {} created for transaction {}", paymentOrder.getRazorpayOrderId(), savedTransaction.getId());
        return mapToTransactionResponse(savedTransaction);
    }

    public void completeTransferAfterPayment(String transactionId) {
        Transaction transaction = repo.findById(transactionId)
                .orElseThrow(() -> new RuntimeException("Transaction not found for id: " + transactionId));

        if (transaction.getTransactionStatus() != TransactionStatus.PAYMENT_PENDING) {
            log.info("Skipping payment completion for transaction {} because status is {}", transactionId, transaction.getTransactionStatus());
            return;
        }

        try {
            accountServiceClient.deductBalance(
                    transaction.getSenderAccountNumber(),
                    transaction.getAmount()
            );

            accountServiceClient.creditBalance(
                    transaction.getReceiverAccountNumber(),
                    transaction.getAmount()
            );

            transaction.setTransactionStatus(TransactionStatus.COMPLETED);
            transaction.setCompletedAt(LocalDateTime.now());
            repo.save(transaction);

            TransactionCompletedEvent event = TransactionCompletedEvent.builder()
                    .transactionId(transaction.getId())
                    .senderAccountNumber(transaction.getSenderAccountNumber())
                    .receiverAccountNumber(transaction.getReceiverAccountNumber())
                    .amount(transaction.getAmount())
                    .description(transaction.getDescription())
                    .build();

            kafkaTemplate.send(TRANSACTION_COMPLETED_TOPIC, transaction.getId(), event);
            log.info("Transaction payment completed and bank transfer effected for {}", transaction.getId());
        } catch (Exception e) {
            log.error("Error completing transfer after payment for transaction {}", transactionId, e);
            transaction.setTransactionStatus(TransactionStatus.FAILED);
            transaction.setFailureMessage("Payment captured but ledger update failed");
            repo.save(transaction);
            throw e;
        }
    }

    public void failTransferAfterPayment(String transactionId, String reason) {
        Transaction transaction = repo.findById(transactionId)
                .orElseThrow(() -> new RuntimeException("Transaction not found for id: " + transactionId));

        if (transaction.getTransactionStatus() == TransactionStatus.COMPLETED) {
            return;
        }

        transaction.setTransactionStatus(TransactionStatus.FAILED);
        transaction.setFailureMessage(reason);
        repo.save(transaction);

        Map<String, Object> refundEvent = new HashMap<>();
        refundEvent.put("transactionId", transaction.getId());
        refundEvent.put("accountNumber", transaction.getSenderAccountNumber());
        refundEvent.put("amount", transaction.getAmount());
        refundEvent.put("reason", reason);
        kafkaTemplate.send(TRANSACTION_REFUNDED_TOPIC, transaction.getId(), refundEvent);
        log.info("Payment failed event processed for transaction {}", transaction.getId());
    }

    public @Nullable TransactionResponse getTransaction(String transactionId) {

        Transaction transaction = repo.findById(transactionId).orElseThrow(
                ()-> new RuntimeException("Transaction not found for id: " + transactionId)
        );

        return mapToTransactionResponse(transaction);
    }

    private TransactionResponse mapToTransactionResponse(Transaction request) {

        return TransactionResponse.builder()
                .id(request.getId())
                .senderAccountNumber(request.getSenderAccountNumber())
                .receiverAccountNumber(request.getReceiverAccountNumber())
                .amount(request.getAmount())
                .trasactionType(request.getTrasactionType())
                .transactionStatus(request.getTransactionStatus())
                .referenceNumber(request.getReferenceNumber())
                .description(request.getDescription())
                .failureMessage(request.getFailureMessage())
                .createdAt(request.getCreatedAt())
                .completedAt(request.getCompletedAt())
                .build();


    }

    public List<TransactionResponse> getTransactionHistory(String accountNumber) {

        List<Transaction> transactions = repo.findBySenderAccountNumberOrReceiverAccountNumberOrderByCreatedAtDesc(accountNumber, accountNumber);

        return transactions.stream()
                .map(this::mapToTransactionResponse)
                .collect(Collectors.toList());
    }

    public @Nullable TransactionResponse verifyOTP(String transactionId, String otp) {
        log.info("Verifying OTP for transactionId: {}", transactionId);
        // Implementation for OTP verification
        Transaction transaction = repo.findById(transactionId).orElseThrow(
                () -> new RuntimeException("Transaction not found for id: " + transactionId)
        );

        if(transaction.getTransactionStatus() != TransactionStatus.PENDING_VERIFICATION){
            log.info("Transaction: {} is not in PENDING_VERIFICATION status", transactionId);
            return mapToTransactionResponse(transaction);
        }

        String key = "transaction:"+transactionId+":"+"otp";
        String storedOtp = (String) redisTemplate.opsForValue().get(key);
        System.out.println("Key: " + key + ", Stored OTP: " + storedOtp);
        if(storedOtp == null){
            log.warn("OTP has expired for transactionId: {}", transactionId);
            compensateTransaction(transaction, "OTP has expired, transaction failed and amount refunded");
            return mapToTransactionResponse(transaction);
        }
        if(!storedOtp.equals(otp)){
            log.warn("Invalid OTP, blocking account and compensating for transactionId: {}", transactionId);
            blockAccountAndCompensate(transaction, "Invalid OTP, account blocked for security reasons and amount refunded");
            return mapToTransactionResponse(transaction);
        }

        // correct otp - complete transaction
        log.warn("Correct OTP received for transactionId: {}", transactionId);
        redisTemplate.delete(key);
        completeTransaction(transaction);

        return mapToTransactionResponse(transaction);
    }

    private void completeTransaction(Transaction transaction) {
        log.info("Completing transaction: {}", transaction.getId());
        transaction.setTransactionStatus(TransactionStatus.COMPLETED);
        transaction.setCompletedAt(LocalDateTime.now());
        repo.save(transaction);

        // publish transaction complete event to account service
        TransactionCompletedEvent event = TransactionCompletedEvent.builder()
                .transactionId(transaction.getId())
                .senderAccountNumber(transaction.getSenderAccountNumber())
                .receiverAccountNumber(transaction.getReceiverAccountNumber())
                .amount(transaction.getAmount())
                .description(transaction.getDescription())
                .build();

        kafkaTemplate.send(TRANSACTION_COMPLETED_TOPIC, transaction.getId(), event);
        log.info("Transaction completed event published for transaction: {}", transaction.getId());
    }

    private void blockAccountAndCompensate(Transaction transaction, String failedReason) {
        log.info("Blocking account and compensating refund for transaction: {}", transaction.getId());

        // publish fraud detected event to Account Service to block the account
        Map<String, Object> fraudEvent = new HashMap<>();
        fraudEvent.put("accountNumber", transaction.getSenderAccountNumber());
        fraudEvent.put("transactionId", transaction.getId());
        fraudEvent.put("reason", failedReason);
        kafkaTemplate.send(FRAUD_DETECTED_TOPIC, transaction.getId(), fraudEvent);
        
        compensateTransaction(transaction, failedReason);
        log.info("Fraud detected event published for transaction: {}", transaction.getId());
    }

    private void compensateTransaction(Transaction transaction, String failedReason) {
        log.info("Compensating refund for transaction: {}", transaction.getId());
        String accountNumber = transaction.getSenderAccountNumber();
        BigDecimal amount = transaction.getAmount();

        accountServiceClient.creditBalance(accountNumber, amount);

        transaction.setTransactionStatus(TransactionStatus.FAILED);
        transaction.setFailureMessage(failedReason);
        repo.save(transaction);

        // publish amount refunded event to notification service

        Map<String, Object> refundEvent = new HashMap<>();

        refundEvent.put("transactionId", transaction.getId());
        refundEvent.put("accountNumber", accountNumber);
        refundEvent.put("amount", amount);
        refundEvent.put("reason", failedReason);

        kafkaTemplate.send(TRANSACTION_REFUNDED_TOPIC, transaction.getId(), refundEvent);
        log.info("Refund completed for transaction: {}", transaction.getId());
    }

    public void processFraudCleanCheck(String transactionId) {
        log.info("Processing fraud clean check for transaction: {}", transactionId);

        Transaction transaction = repo.findById(transactionId).orElseThrow(
                () -> new RuntimeException("Transaction not found for id: " + transactionId)
        );
        completeTransaction(transaction);


    }
}
