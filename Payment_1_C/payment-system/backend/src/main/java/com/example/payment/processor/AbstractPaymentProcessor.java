package com.example.payment.processor;

import com.example.payment.domain.PaymentResult;
import com.example.payment.exception.InvalidPaymentPayloadException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Template Method base class. Every payment method follows the same five
 * steps (parse -> validate -> map to gateway request -> call gateway -> map
 * gateway response back to a PaymentResult); only the types and the
 * per-step logic differ. Concrete processors (CardPaymentProcessor,
 * PaypalPaymentProcessor, ...) implement the steps and get the orchestration
 * for free, which keeps them small and avoids copy-pasted boilerplate as
 * more payment methods are added.
 *
 * @param <TRequest>    this method's own request DTO, parsed from the raw JSON body
 * @param <TGatewayReq> the request shape the external gateway/API expects
 * @param <TGatewayResp> the response shape the external gateway/API returns
 */
public abstract class AbstractPaymentProcessor<TRequest, TGatewayReq, TGatewayResp> implements PaymentProcessor {

    protected final ObjectMapper objectMapper;

    protected AbstractPaymentProcessor(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public final PaymentResult process(JsonNode rawPayload) {
        TRequest request = parseRequest(rawPayload);
        validate(request);
        TGatewayReq gatewayRequest = toGatewayRequest(request);
        TGatewayResp gatewayResponse = callGateway(gatewayRequest);
        return toPaymentResult(gatewayResponse);
    }

    protected abstract TRequest parseRequest(JsonNode rawPayload);

    protected abstract void validate(TRequest request);

    protected abstract TGatewayReq toGatewayRequest(TRequest request);

    protected abstract TGatewayResp callGateway(TGatewayReq gatewayRequest);

    protected abstract PaymentResult toPaymentResult(TGatewayResp gatewayResponse);

    protected TRequest readValue(JsonNode rawPayload, Class<TRequest> targetType) {
        try {
            return objectMapper.treeToValue(rawPayload, targetType);
        } catch (JsonProcessingException e) {
            throw new InvalidPaymentPayloadException(
                    "Payload does not match the " + getMethodType() + " request shape: " + e.getOriginalMessage(), e);
        }
    }
}
