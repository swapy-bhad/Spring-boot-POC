package com.example.payment.model;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record Money(
        @NotNull @DecimalMin("0.01") BigDecimal value,
        @NotBlank String currency) {}
