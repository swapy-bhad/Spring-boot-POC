package com.example.payment.exception;

public class UnsupportedPaymentMethodException extends RuntimeException {
    public UnsupportedPaymentMethodException(String methodType) {
        super("Unsupported payment method: " + methodType);
    }
}
