package com.banking.accountservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class AccountEventConsumer {

    private final AccountService accountService;

    @KafkaListener(topics = "transaction.completed", groupId = "account-service-group")
    public void consumeTransactionCompleted(@Payload Map<String, Object> payload) {
        String accountNumber = (String) payload.get("receiverAccountNumber");
        BigDecimal amount = BigDecimal.valueOf(((Number) payload.get("amount")).doubleValue());
        accountService.creditBalance(accountNumber, amount);
    }

    @KafkaListener(topics = "fraud.detected", groupId = "account-service-group")
    public void consumeFraudDetection(@Payload Map<String, Object> payload){

            String accountNumber = (String) payload.get("accountNumber");
            accountService.blockAccount(accountNumber);
    }
}
