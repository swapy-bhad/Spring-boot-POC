package com.example.payment.dto.card;

/** Shape the (fictional) card gateway's own API expects - amount in minor units, its own field names. */
public record CardGatewayRequest(String token, String expiry, long amountMinorUnits, String currencyCode) {
}
