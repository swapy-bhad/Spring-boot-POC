package com.example.payment.model;

import java.math.BigDecimal;

public record GatewayRequest(
        String paymentId,
        String merchantId,
        String orderId,
        BigDecimal amount,
        String currency,
        Object gatewayPayload) {}
