package com.example.payment.processor;

import com.example.payment.domain.PaymentRequest;
import com.example.payment.domain.PaymentResult;

/**
 * Template Method base class. With the typed controller, Spring/Jackson
 * already parsed the body into the right subtype and Bean Validation
 * already ran on it before process() is called - so the only steps left
 * that differ per payment method are: map to the gateway's own contract,
 * call the gateway, map the response back. Concrete processors
 * (CardPaymentProcessor, PaypalPaymentProcessor, ...) implement those three
 * steps and get the orchestration for free.
 *
 * @param <TRequest>     this method's own request type (already parsed and validated)
 * @param <TGatewayReq>  the request shape the external gateway/API expects
 * @param <TGatewayResp> the response shape the external gateway/API returns
 */
public abstract class AbstractPaymentProcessor<TRequest extends PaymentRequest, TGatewayReq, TGatewayResp>
        implements PaymentProcessor<TRequest> {

    @Override
    public final PaymentResult process(TRequest request) {
        TGatewayReq gatewayRequest = toGatewayRequest(request);
        TGatewayResp gatewayResponse = callGateway(gatewayRequest);
        return toPaymentResult(gatewayResponse);
    }

    protected abstract TGatewayReq toGatewayRequest(TRequest request);

    protected abstract TGatewayResp callGateway(TGatewayReq gatewayRequest);

    protected abstract PaymentResult toPaymentResult(TGatewayResp gatewayResponse);
}
