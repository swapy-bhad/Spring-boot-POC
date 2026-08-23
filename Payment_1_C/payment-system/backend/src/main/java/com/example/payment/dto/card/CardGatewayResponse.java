package com.example.payment.dto.card;

public record CardGatewayResponse(String gatewayTransactionId, String status, String message) {
}
