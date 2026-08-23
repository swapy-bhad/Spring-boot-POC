package com.example.payment.processor;

import com.example.payment.domain.PaymentRequest;
import com.example.payment.domain.PaymentResult;
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
 *
 * Keeps two views of the same processors:
 *  - by method type (String) - used by the filter to validate the header,
 *    and by the config controller to list supported methods.
 *  - by request class - used to dispatch an already-deserialized,
 *    already-validated PaymentRequest to the processor that declared it
 *    via getSupportedRequestType().
 */
@Component
public class PaymentProcessorRegistry {

    private final Map<String, PaymentProcessor<?>> processorsByType;
    private final Map<Class<?>, PaymentProcessor<?>> processorsByRequestClass;

    public PaymentProcessorRegistry(List<PaymentProcessor<?>> processors) {
        this.processorsByType = processors.stream()
                .collect(Collectors.toUnmodifiableMap(
                        p -> p.getMethodType().toUpperCase(),
                        Function.identity()));
        this.processorsByRequestClass = processors.stream()
                .collect(Collectors.toUnmodifiableMap(
                        PaymentProcessor::getSupportedRequestType,
                        Function.identity()));
    }

    public boolean isSupported(String methodType) {
        return methodType != null && processorsByType.containsKey(methodType.toUpperCase());
    }

    public Set<String> getSupportedMethods() {
        return processorsByType.keySet();
    }

    /** Method type declared by the processor registered for this request's own concrete class. */
    public String methodTypeFor(PaymentRequest request) {
        return resolve(request).getMethodType();
    }

    @SuppressWarnings("unchecked")
    public <T extends PaymentRequest> PaymentResult process(T request) {
        PaymentProcessor<T> processor = (PaymentProcessor<T>) resolve(request);
        return processor.process(request);
    }

    private PaymentProcessor<?> resolve(PaymentRequest request) {
        PaymentProcessor<?> processor = processorsByRequestClass.get(request.getClass());
        if (processor == null) {
            throw new UnsupportedPaymentMethodException(request.getClass().getSimpleName());
        }
        return processor;
    }
}
