package com.example.payment.dto.card;

import com.example.payment.domain.PaymentRequest;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

/**
 * CARD's own request shape. Nothing about this needs to match PayPal's
 * fields, or anything the generic controller understands. Implementing
 * PaymentRequest is what makes Jackson eligible to deserialize a
 * {"type": "CARD", ...} body straight into this record; the "type"
 * property itself is consumed by the type resolver and never touches this
 * constructor.
 */
public record CardPaymentRequest(
        @NotBlank String cardToken,
        @NotBlank String expiryMonth,
        @NotBlank String expiryYear,
        @NotNull @Positive BigDecimal amount,
        @NotBlank String currency
) implements PaymentRequest {
}
