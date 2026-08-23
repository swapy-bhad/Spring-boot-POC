package com.example.payment.processor;

import com.example.payment.domain.PaymentResult;
import com.example.payment.domain.PaymentStatus;
import com.example.payment.dto.paypal.PaypalGatewayRequest;
import com.example.payment.dto.paypal.PaypalGatewayResponse;
import com.example.payment.dto.paypal.PaypalPaymentRequest;
import com.example.payment.exception.InvalidPaymentPayloadException;
import com.example.payment.gateway.PaypalGatewayClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class PaypalPaymentProcessor
        extends AbstractPaymentProcessor<PaypalPaymentRequest, PaypalGatewayRequest, PaypalGatewayResponse> {

    public static final String TYPE = "PAYPAL";

    private final PaypalGatewayClient gatewayClient;
    private final Validator validator;

    public PaypalPaymentProcessor(ObjectMapper objectMapper, PaypalGatewayClient gatewayClient, Validator validator) {
        super(objectMapper);
        this.gatewayClient = gatewayClient;
        this.validator = validator;
    }

    @Override
    public String getMethodType() {
        return TYPE;
    }

    @Override
    protected PaypalPaymentRequest parseRequest(JsonNode rawPayload) {
        return readValue(rawPayload, PaypalPaymentRequest.class);
    }

    @Override
    protected void validate(PaypalPaymentRequest request) {
        Set<ConstraintViolation<PaypalPaymentRequest>> violations = validator.validate(request);
        if (!violations.isEmpty()) {
            throw new InvalidPaymentPayloadException(violations.toString());
        }
    }

    @Override
    protected PaypalGatewayRequest toGatewayRequest(PaypalPaymentRequest request) {
        return new PaypalGatewayRequest(
                request.paypalOrderId(), request.payerId(), request.amount().toPlainString(), request.currency());
    }

    @Override
    protected PaypalGatewayResponse callGateway(PaypalGatewayRequest gatewayRequest) {
        return gatewayClient.capture(gatewayRequest);
    }

    @Override
    protected PaymentResult toPaymentResult(PaypalGatewayResponse response) {
        PaymentStatus status = "COMPLETED".equals(response.state()) ? PaymentStatus.SUCCESS : PaymentStatus.FAILED;
        return new PaymentResult(response.captureId(), status, response.note());
    }
}
