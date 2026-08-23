package com.example.payment.dto.card;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

/**
 * CARD's own request shape. Nothing about this needs to match PayPal's
 * fields, or anything the generic controller understands.
 */
public record CardPaymentRequest(
        @NotBlank String cardToken,
        @NotBlank String expiryMonth,
        @NotBlank String expiryYear,
        @NotNull @Positive BigDecimal amount,
        @NotBlank String currency
) {
}
