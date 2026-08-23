package com.example.payment.web;

import com.example.payment.fieldconfig.FieldDefinition;
import com.example.payment.fieldconfig.PaymentMethodFieldConfigService;
import com.example.payment.processor.PaymentProcessorRegistry;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Set;

/**
 * Powers the data-driven frontend: the UI asks "which methods exist" and
 * "what fields does this method need" instead of hard-coding a form per
 * method. Adding CARD/PAYPAL's sibling here required zero React changes.
 */
@RestController
@RequestMapping("/api/payment-methods")
public class PaymentMethodConfigController {

    private final PaymentProcessorRegistry registry;
    private final PaymentMethodFieldConfigService fieldConfigService;

    public PaymentMethodConfigController(PaymentProcessorRegistry registry,
                                          PaymentMethodFieldConfigService fieldConfigService) {
        this.registry = registry;
        this.fieldConfigService = fieldConfigService;
    }

    @GetMapping
    public Set<String> supportedMethods() {
        return registry.getSupportedMethods();
    }

    @GetMapping("/{method}/fields")
    public List<FieldDefinition> fields(@PathVariable String method) {
        return fieldConfigService.getFields(method);
    }
}
