package com.example.payment.mapper;

import com.example.payment.model.GatewayRequest;
import com.example.payment.model.PaymentContext;
import org.springframework.stereotype.Component;
import java.util.Map;

@Component
public class CardGatewayMapper {
    public GatewayRequest map(PaymentContext c) {
        var d = c.paymentDetails();
        Map<String, Object> payload = Map.of(
                "transaction_type", "AUTH",
                "amount", c.amount().value(),
                "currency", c.amount().currency(),
                "card_token", d.get("cardToken").asText(),
                "expiry_month", d.get("expiryMonth").asInt(),
                "expiry_year", d.get("expiryYear").asInt());
        return new GatewayRequest(c.paymentId(), c.merchantId(), c.orderId(),
                c.amount().value(), c.amount().currency(), payload);
    }
}
