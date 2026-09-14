package com.banking.frauddetectionservice.service;

import com.banking.frauddetectionservice.event.TransactionInitiatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class FraudDetectionEventConsumer {

    private final FraudDetectionService fraudDetectionService;

    /*
    * Listens to transaction.initiated topic.
    * Every Transaction goes through fraud check before completing.
    * */

    @KafkaListener(topics = "transaction.initiated", groupId = "fraud-detection-group")
    public void consumeTransactionInitiated(
            @Payload TransactionInitiatedEvent payload
            ){
        log.info("Received transcation for fraud check: {}" + payload.getTransactionId());

        try{

            fraudDetectionService.checkTransaction(payload);

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
