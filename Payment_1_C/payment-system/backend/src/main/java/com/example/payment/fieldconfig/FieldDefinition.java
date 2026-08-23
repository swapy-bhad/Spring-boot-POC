package com.example.payment.fieldconfig;

import java.util.List;

/**
 * Describes one input field for a given payment method, in a shape a UI can
 * render generically without knowing anything about payments. type is one
 * of "text" | "number" | "select"; options is only used for "select".
 */
public record FieldDefinition(
        String name,
        String label,
        String type,
        boolean required,
        String pattern,
        List<String> options
) {
}
