package com.example.payment.fieldconfig;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Loads per-payment-method field definitions from classpath JSON
 * (src/main/resources/field-config/<method>.json) so the set of fields a
 * payment method needs is data, not code. A new payment method's fields are
 * added by dropping in a new JSON file - no Java or frontend changes.
 *
 * Swapping this to read from a database table instead (e.g. so ops can
 * tweak fields without a redeploy) only means changing this one class.
 */
@Service
public class PaymentMethodFieldConfigService {

    private final ObjectMapper objectMapper;
    private final Map<String, List<FieldDefinition>> cache = new ConcurrentHashMap<>();

    public PaymentMethodFieldConfigService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public List<FieldDefinition> getFields(String methodType) {
        return cache.computeIfAbsent(methodType.toUpperCase(), this::loadFromClasspath);
    }

    private List<FieldDefinition> loadFromClasspath(String methodType) {
        String path = "field-config/" + methodType.toLowerCase() + ".json";
        try (InputStream in = new ClassPathResource(path).getInputStream()) {
            return objectMapper.readValue(in, objectMapper.getTypeFactory()
                    .constructCollectionType(List.class, FieldDefinition.class));
        } catch (IOException e) {
            throw new IllegalStateException("No field config found for payment method: " + methodType, e);
        }
    }
}
