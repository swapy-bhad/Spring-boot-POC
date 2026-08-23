package com.example.payment.processor;

import com.example.payment.domain.PaymentResult;
import com.example.payment.domain.PaymentStatus;
import com.example.payment.dto.card.CardGatewayRequest;
import com.example.payment.dto.card.CardGatewayResponse;
import com.example.payment.dto.card.CardPaymentRequest;
import com.example.payment.gateway.CardGatewayClient;
import org.springframework.stereotype.Component;

@Component
public class CardPaymentProcessor extends AbstractPaymentProcessor<CardPaymentRequest, CardGatewayRequest, CardGatewayResponse> {

    public static final String TYPE = "CARD";

    private final CardGatewayClient gatewayClient;

    public CardPaymentProcessor(CardGatewayClient gatewayClient) {
        this.gatewayClient = gatewayClient;
    }

    @Override
    public String getMethodType() {
        return TYPE;
    }

    @Override
    public Class<CardPaymentRequest> getSupportedRequestType() {
        return CardPaymentRequest.class;
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
