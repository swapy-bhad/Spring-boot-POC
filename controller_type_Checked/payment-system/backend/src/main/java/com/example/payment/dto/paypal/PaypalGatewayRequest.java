package com.example.payment.dto.paypal;

public record PaypalGatewayRequest(String orderId, String payerId, String value, String currencyCode) {
}
