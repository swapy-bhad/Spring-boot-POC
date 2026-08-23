package com.example.payment.gateway;

import com.example.payment.dto.paypal.PaypalGatewayRequest;
import com.example.payment.dto.paypal.PaypalGatewayResponse;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Adapter over PayPal's own APIs. In production this would call PayPal's
 * REST APIs (order capture, etc.) and translate their JSON contract into
 * PaypalGatewayResponse. Stubbed here so the sample runs standalone.
 */
@Component
public class PaypalGatewayClient {

    public PaypalGatewayResponse capture(PaypalGatewayRequest request) {
        String captureId = "PP-" + UUID.randomUUID();
        return new PaypalGatewayResponse(captureId, "COMPLETED", "Captured");
    }
}
