package com.example.payment.strategy;

import com.example.payment.model.PaymentContext;
import com.example.payment.model.PaymentMethod;
import com.example.payment.model.PaymentResponse;

public interface PaymentMethodHandler {
    PaymentMethod supportedMethod();
    PaymentResponse process(PaymentContext context);
}
