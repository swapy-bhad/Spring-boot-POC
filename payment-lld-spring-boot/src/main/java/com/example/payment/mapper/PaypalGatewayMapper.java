package com.example.payment.mapper;

import com.example.payment.model.GatewayRequest;
import com.example.payment.model.PaymentContext;
import org.springframework.stereotype.Component;
import java.util.Map;

@Component
public class PaypalGatewayMapper {
    public GatewayRequest map(PaymentContext c) {
        var d = c.paymentDetails();
        Map<String, Object> payload = Map.of(
                "intent", "CAPTURE",
                "purchase_units", java.util.List.of(Map.of(
                        "reference_id", c.orderId(),
                        "amount", Map.of(
                                "currency_code", c.amount().currency(),
                                "value", c.amount().value().toPlainString()))),
                "paypal_order_id", d.get("paypalOrderId").asText(),
                "payer_id", d.get("payerId").asText());
        return new GatewayRequest(c.paymentId(), c.merchantId(), c.orderId(),
                c.amount().value(), c.amount().currency(), payload);
    }
}
