package com.example.payment.model;

import java.math.BigDecimal;

public record PaymentResponse(
        String paymentId,
        PaymentMethod paymentMethod,
        PaymentStatus status,
        BigDecimal amount,
        String currency,
        String gatewayTransactionId,
        String message) {}
