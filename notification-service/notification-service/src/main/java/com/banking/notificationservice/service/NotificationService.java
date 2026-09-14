package com.banking.notificationservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class NotificationService {

    @KafkaListener(topics = "transaction.otp.generated", groupId = "notification-service-group")
    public void consumeOtpGeneratedEvent(@Payload Map<String, Object> payload) {
        log.info("Consuming OTP generated event: {}", payload);

        String transactionId = payload.get("transactionId").toString();
        String otp = payload.get("otp").toString();
        String accountNumber = payload.get("accountNumber").toString();
        String reason = payload.get("reason").toString();
        BigDecimal amount = new BigDecimal(payload.get("amount").toString());

        sendAlert(accountNumber, "TRANSACTION VERIFICATION REQUIRED",
                String.format(
                        "Suspicious activity detected on your account. "+
                        "Reason: %s "+
                        "A translation of %s is pending verification. " +
                        "Your OTP is: %s valid for 5 minutes. "+
                        "If this was not you, ignore the message.", reason, transactionId, otp
                )
                );
    }
    @KafkaListener(topics = "transaction.completed", groupId = "notification-service-group")
    public void consumeTransactionCompletedEvent(@Payload Map<String, Object> payload) {
        log.info("{} - Consuming Transaction completed event: {}", "notification-service", payload);

        try{
            String senderAccountNumber = payload.get("senderAccountNumber").toString();
            String receiverAccountNumber = payload.get("receiverAccountNumber").toString();
            String amount = payload.get("amount").toString();

            sendAlert(senderAccountNumber, "DEBIT ALERT",
                    String.format("%s debited from account %s", amount, senderAccountNumber)
            );

            // credi alert
            sendAlert(senderAccountNumber, "CREDIT ALERT",
                    String.format("%s credited to account %s", amount, receiverAccountNumber)
            );
        } catch (Exception e) {
            log.error("Error sending transcation notification: {}", e.getMessage());
            throw new RuntimeException(e);
        }
    }

    @KafkaListener(topics = "fraud.detected", groupId = "notification-service-group")
    public void consumeFraudDetectEvent(@Payload Map<String, Object> payload) {
        log.info("Consuming Fraud detected event: {}", payload);

        try{
            String accountNumber = payload.get("accountNumber").toString();
            String reason = payload.get("reason").toString();

            sendAlert(accountNumber, "FRAUD DETECTED",
                    String.format("Fraudulent activity detected on your account. Reason: %s", reason)
            );
        } catch (Exception e) {
            log.error("Error sending fraud detection notification: {}", e.getMessage());
            throw new RuntimeException(e);
        }
    }

    @KafkaListener(topics = "transaction.refunded", groupId = "notification-service-group")
    public void consumeTransactionRefundedEvent(@Payload Map<String, Object> payload) {
        log.info("Consuming Transaction refunded event: {}", payload);

        try{
            String accountNumber = payload.get("accountNumber").toString();
            String amount = payload.get("amount").toString();
            String reason = payload.get("reason").toString();

            sendAlert(accountNumber, "TRANSACTION REFUNDED",
                    String.format("Your transaction has been refunded. Amount: %s. Reason: %s", amount, reason)
            );
        } catch (Exception e) {
            log.error("Error sending transaction refunded notification: {}", e.getMessage());
            throw new RuntimeException(e);
        }
    }

    @KafkaListener(topics = "payment.completed", groupId = "notification-service-group")
    public void consumePaymentCompletedEvent(@Payload Map<String, Object> payload) {
        log.info("Consuming Payment completed event: {}", payload);

        try{
            String accountNumber = payload.get("accountNumber").toString();
            String amount = payload.get("amount").toString();
            String razorpayPaymentId = payload.get("razorpayPaymentId").toString();

            sendAlert(accountNumber, "PAYMENT COMPLETED",
                    String.format("Your payment has been completed. Amount: %s. Razorpay Payment ID: %s", amount, razorpayPaymentId)
            );
        } catch (Exception e) {
            log.error("Error sending payment completed notification: {}", e.getMessage());
            throw new RuntimeException(e);
        }
    }

    @KafkaListener(topics = "payment.failed", groupId = "notification-service-group")
    public void consumePaymentFailureEvent(@Payload Map<String, Object> payload) {
        log.info("Consuming Payment failure event: {}", payload);

        try{
            String accountNumber = payload.get("accountNumber").toString();
            String amount = payload.get("amount").toString();
            String razorpayPaymentId = payload.get("razorpayPaymentId").toString();

            sendAlert(accountNumber, "PAYMENT FAILURE",
                    String.format("Your payment has failed. Amount: %s. Razorpay Payment ID: %s", amount, razorpayPaymentId)
            );
        } catch (Exception e) {
            log.error("Error sending payment failure notification: {}", e.getMessage());
            throw new RuntimeException(e);
        }
    }

    private void sendAlert(String accountNumber, String subject, String message) {

        log.info("--------------------------------");
        log.info("Account Number: {}", accountNumber);
        log.info("Subject: {}", subject);
        log.info("Message: {}", message);
        log.info("--------------------------------");
    }
}
