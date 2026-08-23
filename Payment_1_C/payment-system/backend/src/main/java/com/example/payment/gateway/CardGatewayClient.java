package com.example.payment.gateway;

import com.example.payment.dto.card.CardGatewayRequest;
import com.example.payment.dto.card.CardGatewayResponse;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Adapter over the external card gateway. In production this would use
 * RestClient/WebClient to call the real gateway's REST or SOAP API and map
 * its response into CardGatewayResponse. Stubbed here so the sample runs
 * standalone.
 */
@Component
public class CardGatewayClient {

    public CardGatewayResponse charge(CardGatewayRequest request) {
        String transactionId = "CARD-" + UUID.randomUUID();
        return new CardGatewayResponse(transactionId, "AUTHORIZED", "Approved");
    }
}
