package com.example.payment.service;

import com.example.payment.model.*;
import com.example.payment.strategy.PaymentHandlerRegistry;
import org.springframework.stereotype.Service;
import java.util.UUID;

@Service
public class PaymentOrchestrator {
    private final PaymentHandlerRegistry registry;

    public PaymentOrchestrator(PaymentHandlerRegistry registry) {
        this.registry = registry;
    }

    public PaymentResponse process(PaymentRequest request) {
        String paymentId = UUID.randomUUID().toString();
        PaymentContext context = PaymentContext.from(paymentId, request);
        return registry.get(request.paymentMethod()).process(context);
    }
}
