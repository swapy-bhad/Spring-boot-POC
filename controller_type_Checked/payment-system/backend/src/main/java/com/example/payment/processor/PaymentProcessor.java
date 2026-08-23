package com.example.payment.processor;

import com.example.payment.domain.PaymentRequest;
import com.example.payment.domain.PaymentResult;

/**
 * Strategy/Provider contract. One implementation per payment method.
 * The controller and registry only ever talk to this interface, so a new
 * payment method is added purely by writing a new implementation - nothing
 * here or in the controller changes (Open/Closed Principle).
 *
 * Generic over this method's own request type: Spring's Jackson converter
 * has already deserialized the raw body into a concrete PaymentRequest
 * subtype (CardPaymentRequest, PaypalPaymentRequest, ...) and run Bean
 * Validation on it by the time process() is called, using the polymorphic
 * "type" discriminator in the JSON body - see PaymentRequest.
 */
public interface PaymentProcessor<T extends PaymentRequest> {

    /**
     * The value that arrives in the X-Payment-Method header for this
     * processor, e.g. "CARD", "PAYPAL". Used both as the registry's
     * header-validation key and to cross-check the header against the
     * deserialized body's actual type.
     */
    String getMethodType();

    /**
     * The concrete PaymentRequest subtype this processor handles, used by
     * the registry to dispatch an already-deserialized request to the
     * right processor without an instanceof chain.
     */
    Class<T> getSupportedRequestType();

    /**
     * Process an already-typed, already-validated request and return a
     * normalized result.
     */
    PaymentResult process(T request);
}
