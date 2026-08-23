package com.example.payment.model;

public record GatewayResponse(
        boolean success,
        String transactionId,
        String message) {}
