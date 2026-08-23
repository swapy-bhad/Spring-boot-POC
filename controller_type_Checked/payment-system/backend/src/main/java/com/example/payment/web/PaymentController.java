package com.example.payment.web;

import com.example.payment.domain.PaymentMethodContext;
import com.example.payment.domain.PaymentRequest;
import com.example.payment.domain.PaymentResult;
import com.example.payment.exception.InvalidPaymentPayloadException;
import com.example.payment.processor.PaymentProcessorRegistry;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * The single, generic payment API - now with a genuinely typed body.
 * @RequestBody PaymentRequest triggers Jackson's polymorphic deserializer
 * (see PaymentRequest's @JsonTypeInfo/@JsonSubTypes), which reads the
 * body's own "type" field and produces a CardPaymentRequest,
 * PaypalPaymentRequest, or whatever a future method registers - fully
 * typed, with Bean Validation already applied via @Valid, before this
 * method body even runs. The controller still doesn't know a single field
 * name from any of them.
 *
 * The X-Payment-Method header (parsed by PaymentMethodHeaderFilter) is kept
 * as a belt-and-suspenders check: it's what the caller declared up front,
 * cheap to validate before the body is even parsed, and now also cross-
 * checked against what the body actually deserialized to, so a caller that
 * sends a CARD body under a PAYPAL header gets a clear 422 instead of a
 * confusing downstream failure.
 */
@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentProcessorRegistry registry;

    public PaymentController(PaymentProcessorRegistry registry) {
        this.registry = registry;
    }

    @PostMapping
    public ResponseEntity<PaymentResult> processPayment(@Valid @RequestBody PaymentRequest request) {
        String headerMethod = PaymentMethodContext.get();
        String bodyMethod = registry.methodTypeFor(request);

        if (!bodyMethod.equalsIgnoreCase(headerMethod)) {
            throw new InvalidPaymentPayloadException(
                    "X-Payment-Method header ('" + headerMethod + "') does not match the request body's type ('" + bodyMethod + "')");
        }

        PaymentResult result = registry.process(request);
        return ResponseEntity.ok(result);
    }
}
