package com.example.payment.domain;

/**
 * Generic result returned to the client regardless of which payment method
 * processed the request. Every processor maps its gateway-specific response
 * into this shape.
 */
public record PaymentResult(String transactionId, PaymentStatus status, String message) {
}
