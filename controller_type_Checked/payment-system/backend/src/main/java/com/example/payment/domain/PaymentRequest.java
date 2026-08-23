package com.example.payment.domain;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.example.payment.dto.card.CardPaymentRequest;
import com.example.payment.dto.paypal.PaypalPaymentRequest;

/**
 * Common type for every payment method's request body. Jackson reads the
 * "type" field in the JSON body and deserializes straight into the matching
 * subtype - CardPaymentRequest, PaypalPaymentRequest, or a future one -
 * before the controller ever sees it. That's what makes the controller
 * parameter a real type instead of JsonNode: Spring's HttpMessageConverter
 * does the polymorphic dispatch, not hand-rolled parsing inside a processor.
 *
 * Deliberately NOT sealed: a `sealed` permits clause would have to be edited
 * every time a method is added, which defeats the "don't touch shared code"
 * goal. The one shared edit this approach does require is registering the
 * new subtype below in @JsonSubTypes - Jackson has no other way to learn
 * that "APPLEPAY" maps to ApplePayPaymentRequest. That's the real trade-off
 * against the JsonNode + header-only design: this one line of shared code
 * you do have to touch, in exchange for a fully typed, Bean-Validated
 * controller boundary.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = CardPaymentRequest.class, name = "CARD"),
        @JsonSubTypes.Type(value = PaypalPaymentRequest.class, name = "PAYPAL")
})
public interface PaymentRequest {
}
