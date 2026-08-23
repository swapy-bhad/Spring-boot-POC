package com.example.payment.strategy;

import com.example.payment.gateway.CardGatewayAdapter;
import com.example.payment.mapper.CardGatewayMapper;
import com.example.payment.model.*;
import com.example.payment.validation.ValidatorRegistry;
import org.springframework.stereotype.Component;

@Component
public class CardPaymentHandler implements PaymentMethodHandler {
    private final ValidatorRegistry validators;
    private final CardGatewayMapper mapper;
    private final CardGatewayAdapter gateway;

    public CardPaymentHandler(ValidatorRegistry validators, CardGatewayMapper mapper,
                              CardGatewayAdapter gateway) {
        this.validators = validators; this.mapper = mapper; this.gateway = gateway;
    }

    public PaymentMethod supportedMethod() { return PaymentMethod.CARD; }

    public PaymentResponse process(PaymentContext context) {
        validators.get(PaymentMethod.CARD).validate(context);
        GatewayResponse result = gateway.authorize(mapper.map(context));
        PaymentStatus status = result.success() ? PaymentStatus.COMPLETED : PaymentStatus.FAILED;
        return new PaymentResponse(context.paymentId(), context.paymentMethod(), status,
                context.amount().value(), context.amount().currency(),
                result.transactionId(), result.message());
    }
}
