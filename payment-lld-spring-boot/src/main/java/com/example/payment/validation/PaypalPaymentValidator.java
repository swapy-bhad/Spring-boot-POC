package com.example.payment.validation;

import com.example.payment.model.PaymentContext;
import com.example.payment.model.PaymentMethod;
import org.springframework.stereotype.Component;

@Component
public class PaypalPaymentValidator implements PaymentValidator {
    public PaymentMethod supportedMethod() { return PaymentMethod.PAYPAL; }

    public void validate(PaymentContext context) {
        var details = context.paymentDetails();
        if (!details.hasNonNull("paypalOrderId")) {
            throw new IllegalArgumentException("paypalOrderId is required for PAYPAL");
        }
        if (!details.hasNonNull("payerId")) {
            throw new IllegalArgumentException("payerId is required for PAYPAL");
        }
    }
}
