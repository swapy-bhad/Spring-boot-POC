package com.example.payment.gateway;

import com.example.payment.model.GatewayRequest;
import com.example.payment.model.GatewayResponse;

public interface PaymentGateway {
    GatewayResponse authorize(GatewayRequest request);
}
