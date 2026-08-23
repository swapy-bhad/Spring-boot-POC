package com.example.payment.web;

import com.example.payment.exception.InvalidPaymentPayloadException;
import com.example.payment.exception.UnsupportedPaymentMethodException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(UnsupportedPaymentMethodException.class)
    public ResponseEntity<Map<String, String>> handleUnsupported(UnsupportedPaymentMethodException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", "UNSUPPORTED_PAYMENT_METHOD", "message", ex.getMessage()));
    }

    @ExceptionHandler(InvalidPaymentPayloadException.class)
    public ResponseEntity<Map<String, String>> handleInvalid(InvalidPaymentPayloadException ex) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(Map.of("error", "INVALID_PAYLOAD", "message", ex.getMessage()));
    }
}
