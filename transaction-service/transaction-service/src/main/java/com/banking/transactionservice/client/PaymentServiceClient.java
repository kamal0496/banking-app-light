package com.banking.transactionservice.client;

import com.banking.transactionservice.dto.PaymentOrderResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "payment-service", url = "${payment.service.url}")
public interface PaymentServiceClient {

    @PostMapping("/api/v1/payments/create-order")
    PaymentOrderResponse createPaymentOrder(@RequestBody com.banking.transactionservice.dto.CreatePaymentRequest request);
}
