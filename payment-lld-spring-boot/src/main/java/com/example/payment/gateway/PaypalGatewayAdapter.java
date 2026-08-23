package com.example.payment.gateway;

import com.example.payment.model.GatewayRequest;
import com.example.payment.model.GatewayResponse;
import org.springframework.stereotype.Component;

@Component
public class PaypalGatewayAdapter implements PaymentGateway {
    public GatewayResponse authorize(GatewayRequest request) {
        // Replace this mock with the real PayPal HTTP client.
        System.out.println("PAYPAL gateway payload: " + request.gatewayPayload());
        return new GatewayResponse(true, "PP-TXN-" + request.paymentId(), "PayPal authorized");
    }
}
