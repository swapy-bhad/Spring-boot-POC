package com.example.payment.validation;

import com.example.payment.model.PaymentMethod;
import org.springframework.stereotype.Component;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Component
public class ValidatorRegistry {
    private final Map<PaymentMethod, PaymentValidator> validators = new EnumMap<>(PaymentMethod.class);

    public ValidatorRegistry(List<PaymentValidator> validatorList) {
        validatorList.forEach(v -> validators.put(v.supportedMethod(), v));
    }

    public PaymentValidator get(PaymentMethod method) {
        PaymentValidator validator = validators.get(method);
        if (validator == null) throw new IllegalArgumentException("No validator for " + method);
        return validator;
    }
}
