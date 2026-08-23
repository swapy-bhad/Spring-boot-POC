package com.example.payment.model;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record PaymentRequest(
        @NotNull PaymentMethod paymentMethod,
        @Valid @NotNull Money amount,
        @NotBlank String merchantId,
        @NotBlank String orderId,
        @NotNull JsonNode paymentDetails) {}
