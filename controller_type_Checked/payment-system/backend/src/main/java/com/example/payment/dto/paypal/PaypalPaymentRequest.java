package com.example.payment.dto.paypal;

import com.example.payment.domain.PaymentRequest;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

/** PayPal's own request shape - completely different fields from CARD. */
public record PaypalPaymentRequest(
        @NotBlank String paypalOrderId,
        @NotBlank String payerId,
        @NotNull @Positive BigDecimal amount,
        @NotBlank String currency
) implements PaymentRequest {
}
