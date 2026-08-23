package com.example.payment.strategy;

import com.example.payment.gateway.PaypalGatewayAdapter;
import com.example.payment.mapper.PaypalGatewayMapper;
import com.example.payment.model.*;
import com.example.payment.validation.ValidatorRegistry;
import org.springframework.stereotype.Component;

@Component
public class PaypalPaymentHandler implements PaymentMethodHandler {
    private final ValidatorRegistry validators;
    private final PaypalGatewayMapper mapper;
    private final PaypalGatewayAdapter gateway;

    public PaypalPaymentHandler(ValidatorRegistry validators, PaypalGatewayMapper mapper,
                                PaypalGatewayAdapter gateway) {
        this.validators = validators; this.mapper = mapper; this.gateway = gateway;
    }

    public PaymentMethod supportedMethod() { return PaymentMethod.PAYPAL; }

    public PaymentResponse process(PaymentContext context) {
        validators.get(PaymentMethod.PAYPAL).validate(context);
        GatewayResponse result = gateway.authorize(mapper.map(context));
        PaymentStatus status = result.success() ? PaymentStatus.COMPLETED : PaymentStatus.FAILED;
        return new PaymentResponse(context.paymentId(), context.paymentMethod(), status,
                context.amount().value(), context.amount().currency(),
                result.transactionId(), result.message());
    }
}
