package com.example.payment.dto.paypal;

public record PaypalGatewayResponse(String captureId, String state, String note) {
}
