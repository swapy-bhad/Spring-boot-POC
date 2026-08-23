package com.example.payment.strategy;

import com.example.payment.model.PaymentMethod;
import org.springframework.stereotype.Component;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Component
public class PaymentHandlerRegistry {
    private final Map<PaymentMethod, PaymentMethodHandler> handlers = new EnumMap<>(PaymentMethod.class);

    public PaymentHandlerRegistry(List<PaymentMethodHandler> handlerList) {
        handlerList.forEach(h -> handlers.put(h.supportedMethod(), h));
    }

    public PaymentMethodHandler get(PaymentMethod method) {
        PaymentMethodHandler handler = handlers.get(method);
        if (handler == null) {
            throw new IllegalArgumentException("Unsupported payment method: " + method);
        }
        return handler;
    }
}
