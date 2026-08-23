package com.example.payment.processor;

import com.example.payment.exception.UnsupportedPaymentMethodException;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Spring injects every PaymentProcessor bean found on the classpath into
 * this list automatically - registering a new payment method never means
 * editing this class, only adding a new @Component that implements
 * PaymentProcessor.
 */
@Component
public class PaymentProcessorRegistry {

    private final Map<String, PaymentProcessor> processorsByType;

    public PaymentProcessorRegistry(List<PaymentProcessor> processors) {
        this.processorsByType = processors.stream()
                .collect(Collectors.toUnmodifiableMap(
                        p -> p.getMethodType().toUpperCase(),
                        Function.identity()));
    }

    public PaymentProcessor resolve(String methodType) {
        if (methodType == null) {
            throw new UnsupportedPaymentMethodException("null");
        }
        PaymentProcessor processor = processorsByType.get(methodType.toUpperCase());
        if (processor == null) {
            throw new UnsupportedPaymentMethodException(methodType);
        }
        return processor;
    }

    public boolean isSupported(String methodType) {
        return methodType != null && processorsByType.containsKey(methodType.toUpperCase());
    }

    public Set<String> getSupportedMethods() {
        return processorsByType.keySet();
    }
}
