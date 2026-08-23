package com.example.payment.processor;

import com.example.payment.domain.PaymentResult;
import com.example.payment.domain.PaymentStatus;
import com.example.payment.dto.paypal.PaypalGatewayRequest;
import com.example.payment.dto.paypal.PaypalGatewayResponse;
import com.example.payment.dto.paypal.PaypalPaymentRequest;
import com.example.payment.gateway.PaypalGatewayClient;
import org.springframework.stereotype.Component;

@Component
public class PaypalPaymentProcessor extends AbstractPaymentProcessor<PaypalPaymentRequest, PaypalGatewayRequest, PaypalGatewayResponse> {

    public static final String TYPE = "PAYPAL";

    private final PaypalGatewayClient gatewayClient;

    public PaypalPaymentProcessor(PaypalGatewayClient gatewayClient) {
        this.gatewayClient = gatewayClient;
    }

    @Override
    public String getMethodType() {
        return TYPE;
    }

    @Override
    public Class<PaypalPaymentRequest> getSupportedRequestType() {
        return PaypalPaymentRequest.class;
    }

    @Override
    protected PaypalGatewayRequest toGatewayRequest(PaypalPaymentRequest request) {
        return new PaypalGatewayRequest(
                request.paypalOrderId(), request.payerId(), request.amount().toPlainString(), request.currency());
    }

    @Override
    protected PaypalGatewayResponse callGateway(PaypalGatewayRequest gatewayRequest) {
        return gatewayClient.capture(gatewayRequest);
    }

    @Override
    protected PaymentResult toPaymentResult(PaypalGatewayResponse response) {
        PaymentStatus status = "COMPLETED".equals(response.state()) ? PaymentStatus.SUCCESS : PaymentStatus.FAILED;
        return new PaymentResult(response.captureId(), status, response.note());
    }
}
