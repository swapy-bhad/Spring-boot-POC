package com.example.payment.web;

import com.example.payment.domain.PaymentMethodContext;
import com.example.payment.domain.PaymentResult;
import com.example.payment.processor.PaymentProcessor;
import com.example.payment.processor.PaymentProcessorRegistry;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * The single, generic payment API. It has no idea what a "card" or a
 * "paypal order" is - it just resolves the method that
 * PaymentMethodHeaderFilter already validated, hands the raw JSON body to
 * that method's processor, and returns whatever normalized result comes
 * back. Adding a new payment method never touches this class.
 */
@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentProcessorRegistry registry;

    public PaymentController(PaymentProcessorRegistry registry) {
        this.registry = registry;
    }

    @PostMapping
    public ResponseEntity<PaymentResult> processPayment(@RequestBody JsonNode payload) {
        String methodType = PaymentMethodContext.get();
        PaymentProcessor processor = registry.resolve(methodType);
        PaymentResult result = processor.process(payload);
        return ResponseEntity.ok(result);
    }
}
