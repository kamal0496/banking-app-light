package com.banking.paymentservice.service;

import com.banking.paymentservice.dto.CreatePaymentRequest;
import com.banking.paymentservice.dto.dto.PaymentOrderResponse;
import com.banking.paymentservice.entity.Payment;
import com.banking.paymentservice.entity.PaymentStatus;
import com.banking.paymentservice.repository.PaymentRepository;
import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import jakarta.validation.Valid;
import org.json.JSONObject;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);

    private final PaymentRepository repository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public PaymentService(PaymentRepository repository, KafkaTemplate<String, Object> kafkaTemplate) {
        this.repository = repository;
        this.kafkaTemplate = kafkaTemplate;
    }

    @Value("${razorpay.key.id}")
    private String keyId;

    @Value("${razorpay.key.secret}")
    private String keySecret;

    private static final String PAYMENT_COMPLETED_TOPIC = "payment.completed";
    private static final String PAYMENT_FAILED_TOPIC = "payment.failed";

    /*
    * 1. Create order in razorpay
    * 2. Save payment in DB
    * 3. Returns order details to frontend
    * 4. Front-end show razorpay checkout
    * 5. User pays
    * 6. Razorpay calls webhook*/

    public PaymentOrderResponse creatPaymentOrder(@Valid CreatePaymentRequest request) throws RazorpayException {
        // Implementation for creating payment order
        log.info("Creating payment order for request: {}", request);

        RazorpayClient razorpayClient = new RazorpayClient(keyId, keySecret);
        //converted amount
        int convertedAmount = request.getAmount().multiply(BigDecimal.valueOf(100)).intValue();

        JSONObject orderRequest = new JSONObject();
        orderRequest.put("amount", convertedAmount);
        orderRequest.put("currency", "INR");
        orderRequest.put("receipt", "receipt_"+ System.currentTimeMillis() +UUID.randomUUID().toString()
                .replace("-", "").substring(0, 10)
        );

        Order razorpayOrder = razorpayClient.orders.create(orderRequest);
        log.info("razorpay order created: {}", razorpayOrder.get("id").toString());

        Payment payment = new Payment();
        payment.setRazorpayOrderId(razorpayOrder.get("id").toString());
        payment.setAmount(request.getAmount());
        payment.setCurrency("INR");
        payment.setStatus(PaymentStatus.CREATED);
        payment.setAccountNumber(request.getAccountNumber());
        payment.setReceiverAccountNumber(request.getReceiverAccountNumber());
        payment.setTransactionId(request.getTransactionId());
        payment.setDescription(request.getDescription());

        Payment savedPayment = repository.save(payment);


        return new PaymentOrderResponse(
                savedPayment.getId(),
                razorpayOrder.get("id").toString(),
                request.getAmount(),
                "INR",
                "CREATED",
                keyId,
                request.getTransactionId(),
                request.getReceiverAccountNumber()
        );
    }

    public String handleWebHook(Map<String, Object> payload) {
        log.info("Handling webhook with payload: {}", payload.get("event"));

        String event = payload.get("event").toString();

        if("payment.captured".equals(event)) {
            handlePaymentSuccess(payload);
        }
        if("payment.failed".equals(event)) {
            handlePaymentFailure(payload);
        }
        return "Webhook handled successfully";
    }

    private void handlePaymentFailure(Map<String, Object> payload) {

        log.info("Handling payment failure: {}", payload);
        try{
            Map<String, Object> paymentData = extractPaymentData(payload);
            String orderId = (String) paymentData.get("order_id");

            Payment payment = repository.findByRazorpayOrderId(orderId).orElseThrow(() -> new RuntimeException("Payment not found : " + orderId));

            payment.setStatus(PaymentStatus.FAILED);
            payment.setFailureReason("payment failed via razorpay");
            repository.save(payment);

            // publish payment completed event to kafka
            Map<String, Object> event = new HashMap<>();
            event.put("paymentId", payment.getId());
            event.put("transactionId", payment.getTransactionId());
            event.put("status", payment.getStatus());
            event.put("accountNumber", payment.getAccountNumber());
            event.put("receiverAccountNumber", payment.getReceiverAccountNumber());
            event.put("amount", payment.getAmount());
            event.put("failureReason", payment.getFailureReason());

            kafkaTemplate.send(PAYMENT_FAILED_TOPIC, payment.getId(), event);
        } catch (Exception e) {
            log.error("Error occurred while handling payment failure: {}", e.getMessage());
            throw new RuntimeException(e);
        }

    }

    private void handlePaymentSuccess(Map<String, Object> payload) {
        try{
            Map<String, Object> paymentData = extractPaymentData(payload);
            String orderId = (String) paymentData.get("order_id");
            String paymentId = (String) paymentData.get("id");

            Payment payment = repository.findByRazorpayOrderId(orderId).orElseThrow(() -> new RuntimeException("Payment not found : " + orderId));

            payment.setRazorpayPaymentId(paymentId);
            payment.setStatus(PaymentStatus.COMPLETED);
            repository.save(payment);

            // publish payment completed event to kafka
            Map<String, Object> event = new HashMap<>();
            event.put("paymentId", payment.getId());
            event.put("transactionId", payment.getTransactionId());
            event.put("status", payment.getStatus());
            event.put("accountNumber", payment.getAccountNumber());
            event.put("receiverAccountNumber", payment.getReceiverAccountNumber());
            event.put("amount", payment.getAmount());
            event.put("razorpayPaymentId", payment.getRazorpayPaymentId());

            kafkaTemplate.send(PAYMENT_COMPLETED_TOPIC,payment.getId(), event);
            log.info("Payment Completed: {}", payment.getId());
            log.info("Payment completed event published to Kafka");

        } catch (Exception e) {
            log.error("Error occurred while handling payment success: {}", e.getMessage());
            throw new RuntimeException(e);
        }
    }

    private Map<String, Object> extractPaymentData(Map<String, Object> payload) {

        Map<String, Object> entity =  (Map<String, Object>) payload.get("payload");
        Map<String, Object> paymentWrapper = (Map<String, Object>) entity.get("payment");

        return (Map<String, Object>) paymentWrapper.get("entity");

    }
}
