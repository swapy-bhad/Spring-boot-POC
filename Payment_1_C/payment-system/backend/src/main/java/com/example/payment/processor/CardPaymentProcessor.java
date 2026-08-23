package com.example.payment.processor;

import com.example.payment.domain.PaymentResult;
import com.example.payment.domain.PaymentStatus;
import com.example.payment.dto.card.CardGatewayRequest;
import com.example.payment.dto.card.CardGatewayResponse;
import com.example.payment.dto.card.CardPaymentRequest;
import com.example.payment.exception.InvalidPaymentPayloadException;
import com.example.payment.gateway.CardGatewayClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class CardPaymentProcessor
        extends AbstractPaymentProcessor<CardPaymentRequest, CardGatewayRequest, CardGatewayResponse> {

    public static final String TYPE = "CARD";

    private final CardGatewayClient gatewayClient;
    private final Validator validator;

    public CardPaymentProcessor(ObjectMapper objectMapper, CardGatewayClient gatewayClient, Validator validator) {
        super(objectMapper);
        this.gatewayClient = gatewayClient;
        this.validator = validator;
    }

    @Override
    public String getMethodType() {
        return TYPE;
    }

    @Override
    protected CardPaymentRequest parseRequest(JsonNode rawPayload) {
        return readValue(rawPayload, CardPaymentRequest.class);
    }

    @Override
    protected void validate(CardPaymentRequest request) {
        Set<ConstraintViolation<CardPaymentRequest>> violations = validator.validate(request);
        if (!violations.isEmpty()) {
            throw new InvalidPaymentPayloadException(violations.toString());
        }
    }

    @Override
    protected CardGatewayRequest toGatewayRequest(CardPaymentRequest request) {
        long amountMinorUnits = request.amount().movePointRight(2).longValueExact();
        String expiry = request.expiryMonth() + "/" + request.expiryYear();
        return new CardGatewayRequest(request.cardToken(), expiry, amountMinorUnits, request.currency());
    }

    @Override
    protected CardGatewayResponse callGateway(CardGatewayRequest gatewayRequest) {
        return gatewayClient.charge(gatewayRequest);
    }

    @Override
    protected PaymentResult toPaymentResult(CardGatewayResponse response) {
        PaymentStatus status = "AUTHORIZED".equals(response.status()) ? PaymentStatus.SUCCESS : PaymentStatus.FAILED;
        return new PaymentResult(response.gatewayTransactionId(), status, response.message());
    }
}
