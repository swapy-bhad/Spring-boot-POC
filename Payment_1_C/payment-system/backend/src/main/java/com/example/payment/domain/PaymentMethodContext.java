package com.example.payment.domain;

/**
 * Holds the payment method resolved from the request header for the
 * duration of the current request thread. Populated by
 * {@link com.example.payment.web.PaymentMethodHeaderFilter} and read by the
 * generic controller, so no controller/service code needs to know about
 * headers at all.
 */
public final class PaymentMethodContext {

    private static final ThreadLocal<String> CURRENT = new ThreadLocal<>();

    private PaymentMethodContext() {
    }

    public static void set(String methodType) {
        CURRENT.set(methodType);
    }

    public static String get() {
        return CURRENT.get();
    }

    public static void clear() {
        CURRENT.remove();
    }
}
