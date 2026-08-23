package com.example.payment.web;

import com.example.payment.exception.InvalidPaymentPayloadException;
import com.example.payment.exception.UnsupportedPaymentMethodException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
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

    /** @Valid failures on the deserialized CardPaymentRequest / PaypalPaymentRequest / etc. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(fe -> fieldErrors.put(fe.getField(), fe.getDefaultMessage()));
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(Map.of("error", "VALIDATION_FAILED", "fields", fieldErrors));
    }

    /** Body has no recognizable "type", an unknown "type", or is otherwise malformed JSON. */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, String>> handleUnreadable(HttpMessageNotReadableException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", "MALFORMED_REQUEST_BODY",
                        "message", "Body is missing a recognized \"type\" field or is not valid JSON for that type"));
    }
}
