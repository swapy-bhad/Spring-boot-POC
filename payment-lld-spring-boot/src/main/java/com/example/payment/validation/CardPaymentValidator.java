package com.example.payment.validation;

import com.example.payment.model.PaymentContext;
import com.example.payment.model.PaymentMethod;
import org.springframework.stereotype.Component;

@Component
public class CardPaymentValidator implements PaymentValidator {
    public PaymentMethod supportedMethod() { return PaymentMethod.CARD; }

    public void validate(PaymentContext context) {
        var details = context.paymentDetails();
        require(details.hasNonNull("cardToken"), "cardToken is required for CARD");
        require(details.hasNonNull("expiryMonth"), "expiryMonth is required for CARD");
        require(details.hasNonNull("expiryYear"), "expiryYear is required for CARD");
    }

    private void require(boolean condition, String message) {
        if (!condition) throw new IllegalArgumentException(message);
    }
}
