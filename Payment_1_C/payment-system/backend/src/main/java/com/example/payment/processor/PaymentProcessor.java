package com.example.payment.processor;

import com.example.payment.domain.PaymentResult;
import com.fasterxml.jackson.databind.JsonNode;

/**
 * Strategy/Provider contract. One implementation per payment method.
 * The controller and registry only ever talk to this interface, so a new
 * payment method is added purely by writing a new implementation - nothing
 * here or in the controller changes (Open/Closed Principle).
 */
public interface PaymentProcessor {

    /**
     * The value that arrives in the X-Payment-Method header for this
     * processor, e.g. "CARD", "PAYPAL". Used as the registry lookup key.
     */
    String getMethodType();

    /**
     * Process a raw JSON payload whose shape is entirely owned by this
     * processor (card fields, PayPal fields, whatever a future method
     * needs) and return a normalized result.
     */
    PaymentResult process(JsonNode rawPayload);
}
