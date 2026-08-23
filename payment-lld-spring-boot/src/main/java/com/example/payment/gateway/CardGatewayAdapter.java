package com.example.payment.gateway;

import com.example.payment.model.GatewayRequest;
import com.example.payment.model.GatewayResponse;
import org.springframework.stereotype.Component;

@Component
public class CardGatewayAdapter implements PaymentGateway {
    public GatewayResponse authorize(GatewayRequest request) {
        // Replace this mock with the real card-gateway HTTP client.
        System.out.println("CARD gateway payload: " + request.gatewayPayload());
        return new GatewayResponse(true, "CARD-TXN-" + request.paymentId(), "Card authorized");
    }
}
