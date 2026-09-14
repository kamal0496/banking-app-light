package com.banking.frauddetectionservice.service;

import com.banking.frauddetectionservice.client.AccountServiceClient;
import com.banking.frauddetectionservice.event.TransactionInitiatedEvent;
import com.banking.frauddetectionservice.model.FraudCheckResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;

@Service
@RequiredArgsConstructor
@Slf4j
public class FraudDetectionService {

    private final AccountServiceClient accountServiceClient;

    private static final String VERIFICATION_REQUIRED_TOPIC = "verification.required";
    private static final String FRAUD_CHECK_CLEAN_RESULT_TOPIC = "fraud.check.clean";

    private final KafkaTemplate<String, Object> kafkaTemplate;

    void checkTransaction(TransactionInitiatedEvent paylod){
        String transactionId = paylod.getTransactionId();
        String accountNumber = paylod.getSenderAccountNumber();
        BigDecimal amount = paylod.getAmount();

        //Fetch real balance from Account Service

        BigDecimal senderBalance = accountServiceClient.getBalance(accountNumber);
        log.info("Checking transaction : {} account: {} amount: {} balance: {}", transactionId, accountNumber, amount, senderBalance);

        FraudCheckResult result = performFraudCheck(accountNumber, amount, senderBalance);

        if(result.isFraud()){
            log.info("Suspicious activity detected  - account: {} reason: {} - requesting OTP verification", accountNumber, result.getReason());

            Map<String, Object> notificationEvent = new HashMap<>();
            notificationEvent.put("transactionId", transactionId);
            notificationEvent.put("accountNumber", accountNumber);
            notificationEvent.put("amount", amount);
            notificationEvent.put("reason", result.getReason());

            kafkaTemplate.send(VERIFICATION_REQUIRED_TOPIC, transactionId, notificationEvent);
            log.info("Notification Event sent to kafka topic: {} data: {}", VERIFICATION_REQUIRED_TOPIC, notificationEvent);

        }else{
            log.info("No fraud has been detect for transactionId: {}", transactionId);

            kafkaTemplate.send(FRAUD_CHECK_CLEAN_RESULT_TOPIC, transactionId, paylod);
            log.info("FRAUD_CLEAN_CHECK_COMPLETED Event sent to kafka topic: {} data: {}", FRAUD_CHECK_CLEAN_RESULT_TOPIC, paylod);
        }


    }

    private FraudCheckResult performFraudCheck(String accountNumber, BigDecimal amount, BigDecimal senderBalance) {
        int random = (int)(Math.random() * 2);

        if(random == 1){
            return new FraudCheckResult(true, "High-risk transaction detected");
        }
        return new FraudCheckResult(false, "Transaction appears legitimate");
    }
}
