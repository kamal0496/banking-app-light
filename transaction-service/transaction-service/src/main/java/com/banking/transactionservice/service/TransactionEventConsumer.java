package com.banking.transactionservice.service;

import com.banking.transactionservice.entity.Transaction;
import com.banking.transactionservice.entity.TransactionStatus;
import com.banking.transactionservice.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.types.Expiration;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
@RequiredArgsConstructor
public class TransactionEventConsumer {

    private final RedisTemplate<String, Object> redisTemplate;
    private final TransactionRepository transactionRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final TransactionService transactionService;

    private static final String TRANSACTION_OTP_GENERATED_TOPIC = "transaction.otp.generated";

    @KafkaListener(topics = "verification.required", groupId = "transaction-service-group")
    public void consumeVerificationRequired(@Payload Map<String, Object> payload){

        try{
            String transactionId = payload.get("transactionId").toString();
            String accountNumber = payload.get("accountNumber").toString();
//            BigDecimal amount = (BigDecimal) payload.get("amount");

            BigDecimal amount = new BigDecimal(payload.get("amount").toString());
            String reason = payload.get("reason").toString();
            log.info("Verification required for account: {} reason: {}", transactionId, reason );

            Transaction transaction = transactionRepository.findById(transactionId)
                    .orElseThrow(() -> new RuntimeException("Transaction not found for id: " + transactionId));

            if(transaction.getTransactionStatus() != TransactionStatus.PROCESSING){
                return;
            }

            String otp = String.format("%06d", (int) (Math.random() * 900000) + 100000);

            String key = "transaction:"+transactionId+":"+"otp";
            redisTemplate.opsForValue().set(key, otp,5, TimeUnit.MINUTES);

            transaction.setTransactionStatus(TransactionStatus.PENDING_VERIFICATION);
            transactionRepository.save(transaction);

            log.info("OTP generated for transaction: {} expires in {} minutes", transaction, 5);

            Map<String, Object> otpEvent = new HashMap<>();
            otpEvent.put("transactionId", transactionId);
            otpEvent.put("otp", otp);
            otpEvent.put("accountNumber", accountNumber);
            otpEvent.put("amount", amount);
            otpEvent.put("reason", reason);

            kafkaTemplate.send(TRANSACTION_OTP_GENERATED_TOPIC, transactionId, otpEvent);
            log.info("OTP event sent for transaction: {} amount: {}", transactionId, amount);

        } catch (Exception e) {
            log.error("Error occurred while consuming verification required event for transaction: {}", payload.get("transactionId"), e);
        }


    }

    @KafkaListener(topics = "fraud.check.clean", groupId = "transaction-service-group")
    public void consumeFraudCleanCheckEvent(@Payload Map<String, Object> payload) {
        // Implementation for consuming fraud clean check event
        log.info("Consuming fraud clean check event for transaction: {}", payload.get("transactionId"));
        try {
            String transactionId = payload.get("transactionId").toString();
            transactionService.processFraudCleanCheck(transactionId);
        } catch (Exception e) {
            log.error("Error occurred while consuming fraud clean check event for transaction: {}", payload.get("transactionId"), e);
            throw new RuntimeException(e);
        }

    }

    @KafkaListener(topics = "payment.completed", groupId = "transaction-service-group")
    public void consumePaymentCompleted(@Payload Map<String, Object> payload) {
        log.info("Consuming payment completed event: {}", payload);
        try {
            String transactionId = payload.get("transactionId") == null ? null : payload.get("transactionId").toString();
            if (transactionId == null || transactionId.isBlank()) {
                log.warn("Ignoring payment.completed event without transactionId: {}", payload);
                return;
            }
            transactionService.completeTransferAfterPayment(transactionId);
        } catch (Exception e) {
            log.error("Error occurred while consuming payment completed event: {}", payload, e);
            throw new RuntimeException(e);
        }
    }

    @KafkaListener(topics = "payment.failed", groupId = "transaction-service-group")
    public void consumePaymentFailed(@Payload Map<String, Object> payload) {
        log.info("Consuming payment failed event: {}", payload);
        try {
            String transactionId = payload.get("transactionId") == null ? null : payload.get("transactionId").toString();
            if (transactionId == null || transactionId.isBlank()) {
                log.warn("Ignoring payment.failed event without transactionId: {}", payload);
                return;
            }
            transactionService.failTransferAfterPayment(transactionId, payload.getOrDefault("failureReason", "Payment failed").toString());
        } catch (Exception e) {
            log.error("Error occurred while consuming payment failed event: {}", payload, e);
            throw new RuntimeException(e);
        }
    }
}
