# Payment LLD - Spring Boot Reference Project

This project demonstrates a production-oriented Low-Level Design for a payment API where payment methods have:
- Different request fields
- Different JSON structures
- Different validation rules
- Different downstream gateways
- Different gateway request/response contracts

## Architecture

```text
Client
  |
  v
PaymentController
  |
  v
PaymentOrchestrator
  |
  +---- PaymentHandlerRegistry
           |
           +---- CardPaymentHandler
           |       |
           |       +---- CardValidator
           |       +---- CardGatewayMapper
           |       +---- CardGatewayAdapter --> Card API
           |
           +---- PaypalPaymentHandler
                   |
                   +---- PaypalValidator
                   +---- PaypalGatewayMapper
                   +---- PaypalGatewayAdapter --> PayPal API
```

## Patterns
- Strategy Pattern: payment-method-specific processing
- Adapter Pattern: gateway-specific integrations
- Registry/Factory-style lookup: selects handler by payment method
- Mapper Pattern: protects core model from vendor JSON contracts

## Run

Requirements:
- Java 21
- Maven 3.9+

```bash
mvn spring-boot:run
```

Swagger UI:
`http://localhost:8080/swagger-ui.html`

## CARD example

```bash
curl -X POST http://localhost:8080/v1/payments \
  -H "Content-Type: application/json" \
  -d '{
    "paymentMethod": "CARD",
    "amount": {"value": 100.00, "currency": "USD"},
    "merchantId": "M123",
    "orderId": "ORD123",
    "paymentDetails": {
      "cardToken": "tok_123",
      "expiryMonth": 12,
      "expiryYear": 2029
    }
  }'
```

## PAYPAL example

```bash
curl -X POST http://localhost:8080/v1/payments \
  -H "Content-Type: application/json" \
  -d '{
    "paymentMethod": "PAYPAL",
    "amount": {"value": 100.00, "currency": "USD"},
    "merchantId": "M123",
    "orderId": "ORD124",
    "paymentDetails": {
      "paypalOrderId": "PP-123",
      "payerId": "PAYER123"
    }
  }'
```

The gateway adapters are intentionally mocked. Replace them with WebClient/RestClient clients when connecting to real gateways.

## Production extensions
For a real payment platform, add:
- Idempotency keys and request hashing
- Persistent payment state and state-transition validation
- Outbox/event publishing
- Kafka/event-driven integration
- Gateway timeouts, retries and circuit breakers
- Distributed tracing/correlation IDs
- Secrets management
- PCI/tokenization controls
- Authentication/authorization
- Contract tests against gateway APIs
- Resilience4j
- Metrics and audit logging


┌─────────────────────────────────────────────────────────────────────┐
│                         PAYMENT SERVICE                             │
│                                                                     │
│  ┌──────────────┐        ┌───────────────────┐                      │
│  │ REST         │        │ Payment           │                      │
│  │ Controller   │───────►│ Orchestrator      │                      │
│  └──────────────┘        └─────────┬─────────┘                      │
│                                    │                                │
│                         ┌──────────▼──────────┐                     │
│                         │ Handler Registry     │                     │
│                         └──────────┬──────────┘                     │
│                                    │                                │
│             ┌──────────────────────┼──────────────────────┐         │
│             │                      │                      │         │
│             ▼                      ▼                      ▼         │
│     ┌────────────────┐    ┌────────────────┐    ┌────────────────┐│
│     │ Card Handler   │    │ PayPal Handler  │    │ Future Handler ││
│     └───────┬────────┘    └───────┬────────┘    └────────────────┘│
│             │                     │                                 │
│       ┌─────▼─────┐         ┌─────▼─────┐                          │
│       │ Validator │         │ Validator │                          │
│       └─────┬─────┘         └─────┬─────┘                          │
│             │                     │                                 │
│       ┌─────▼─────┐         ┌─────▼─────┐                          │
│       │   Mapper  │         │   Mapper  │                          │
│       └─────┬─────┘         └─────┬─────┘                          │
│             │                     │                                 │
│       ┌─────▼─────┐         ┌─────▼─────┐                          │
│       │  Gateway  │         │  Gateway  │                          │
│       │  Adapter  │         │  Adapter  │                          │
│       └───────────┘         └───────────┘                          │
│                                                                     │
│  ┌────────────────┐  ┌────────────────┐  ┌─────────────────────┐  │
│  │ Idempotency    │  │ Payment State  │  │ Repository          │  │
│  │ Service        │  │ Manager        │  │                     │  │
│  └────────────────┘  └────────────────┘  └─────────────────────┘  │
└─────────────────────────────────────────────────────────────────────┘