package com.example.payment.validation;

import com.example.payment.model.PaymentContext;
import com.example.payment.model.PaymentMethod;

public interface PaymentValidator {
    PaymentMethod supportedMethod();
    void validate(PaymentContext context);
}
