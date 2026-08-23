package com.example.payment.model;

import com.fasterxml.jackson.databind.JsonNode;

public record PaymentContext(
        String paymentId,
        PaymentMethod paymentMethod,
        Money amount,
        String merchantId,
        String orderId,
        JsonNode paymentDetails) {

    public static PaymentContext from(String paymentId, PaymentRequest request) {
        return new PaymentContext(paymentId, request.paymentMethod(), request.amount(),
                request.merchantId(), request.orderId(), request.paymentDetails());
    }
}
