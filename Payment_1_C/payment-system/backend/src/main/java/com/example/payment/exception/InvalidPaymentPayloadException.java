package com.example.payment.exception;

public class InvalidPaymentPayloadException extends RuntimeException {
    public InvalidPaymentPayloadException(String message) {
        super(message);
    }

    public InvalidPaymentPayloadException(String message, Throwable cause) {
        super(message, cause);
    }
}
