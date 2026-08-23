Act as a Senior Application Architect and Java/Spring Boot developer.

Design and implement a production-oriented Payment Processing System using Java 21 and Spring Boot 3.x.

BUSINESS REQUIREMENT
--------------------
We have a single payment API, but the payment request and processing flow varies significantly by payment method.

Supported payment methods initially:
1. CARD
2. PAYPAL
3. FUTURE

The controller/API action should remain generic, but the downstream processing must be payment-method-specific.

Examples:

CARD:
- Has card-specific fields such as cardToken, expiryMonth, expiryYear.
- Uses a Card Gateway.
- Card Gateway has its own JSON request/response contract.

PAYPAL:
- Has completely different fields such as paypalOrderId and payerId.
- Uses PayPal APIs.
- PayPal has a completely different JSON request/response structure.

Future payment methods must be easy to add without modifying existing Card or PayPal implementations.

The identifies for differnt payment method in reauest header and can be fetch in interceptor or filter.
Develop sprint boot+react project.
Also check possibility for having data driven approach to render UI and provider design patter or suggest if there is any better appriach.
ALso check if the fields can be configurable to have more generic flow.


ARCHITECTURE
------------
Use the following architecture:

Client
  |
  v
PaymentController
  |
  v
PaymentOrchestrator
  |
  v
PaymentMethodHandler / Strategy
  |
  +---- CardPaymentHandler
  |       |
  |       +---- CardValidator
  |       +---- CardGatewayMapper
  |       +---- CardGatewayAdapter
  |                    |
  |                    v
  |                 Card API
  |
  +---- PaypalPaymentHandler
          |
          +---- PaypalValidator
          +---- PaypalGatewayMapper
          +---- PaypalGatewayAdapter
                       |
                       v
                    PayPal API

DESIGN PATTERNS
---------------
Use:

1. Strategy Pattern
   - Select processing logic based on paymentMethod.
   - PaymentController must NOT contain payment-method-specific logic.
   - Avoid large if/else or switch statements.

2. Adapter Pattern
   - Each external gateway has a separate adapter.
   - Internal application models must not be exposed directly to external gateways.

3. Mapper Pattern
   - Convert the canonical internal PaymentContext into gateway-specific JSON.
   - Card and PayPal payloads must be independently mapped.

4. Registry/Factory Pattern
   - Dynamically resolve the correct PaymentMethodHandler using PaymentMethod.
   - Use Spring dependency injection.

5. State Machine / State Transition Design
   - Payment statuses should be controlled centrally.

API
---
Expose:

POST /v1/payments

The common request should contain:

{
  "paymentMethod": "CARD",
  "amount": {
    "value": 100.00,
    "currency": "USD"
  },
  "merchantId": "M123",
  "orderId": "ORD123",
  "paymentDetails": {}
}

For CARD:

{
  "paymentMethod": "CARD",
  "amount": {
    "value": 100.00,
    "currency": "USD"
  },
  "merchantId": "M123",
  "orderId": "ORD123",
  "paymentDetails": {
    "cardToken": "tok_123",
    "expiryMonth": 12,
    "expiryYear": 2029
  }
}

For PAYPAL:

{
  "paymentMethod": "PAYPAL",
  "amount": {
    "value": 100.00,
    "currency": "USD"
  },
  "merchantId": "M123",
  "orderId": "ORD124",
  "paymentDetails": {
    "paypalOrderId": "PP-123",
    "payerId": "PAYER123"
  }
}

Keep common fields separate from payment-specific fields.

Do NOT create one huge PaymentRequest containing every possible Card, PayPal, Bank, Apple Pay, etc. field.

DOMAIN MODEL
------------
Create:

PaymentMethod:
- CARD
- PAYPAL

PaymentStatus:
- CREATED
- PROCESSING
- AUTHORIZED
- CAPTURED
- COMPLETED
- FAILED
- REFUNDED

Money:
- value
- currency

PaymentContext:
- paymentId
- paymentMethod
- amount
- merchantId
- orderId
- paymentDetails

PaymentResponse:
- paymentId
- paymentMethod
- status
- amount
- currency
- gatewayTransactionId
- message

HANDLER
-------
Create:

PaymentMethodHandler

Methods:

PaymentMethod supportedMethod();

PaymentResponse process(PaymentContext context);

Implement:

CardPaymentHandler
PaypalPaymentHandler

Each handler should:
1. Validate payment-specific fields.
2. Map internal context to gateway request.
3. Invoke its gateway adapter.
4. Normalize the gateway response.
5. Return a common PaymentResponse.

VALIDATION
----------
Create:

PaymentValidator

Methods:

PaymentMethod supportedMethod();

void validate(PaymentContext context);

Implement:

CardPaymentValidator
PaypalPaymentValidator

Common validation should be performed independently from payment-specific validation.

GATEWAY
-------
Create:

PaymentGateway

Methods:

GatewayResponse authorize(GatewayRequest request);

GatewayResponse capture(GatewayRequest request);

GatewayResponse refund(GatewayRequest request);

Implement:

CardGatewayAdapter
PaypalGatewayAdapter

For this project, gateway calls should initially be mocked.

Clearly mark the location where actual WebClient/RestClient calls should be implemented.

MAPPING
-------
Create:

CardGatewayMapper
PaypalGatewayMapper

The Card mapper should generate a Card-specific JSON structure.

Example:

{
  "transaction_type": "AUTH",
  "amount": 100.00,
  "currency": "USD",
  "card_token": "tok_123",
  "expiry_month": 12,
  "expiry_year": 2029
}

The PayPal mapper should generate a completely different structure.

Example:

{
  "intent": "CAPTURE",
  "purchase_units": [
    {
      "reference_id": "ORD123",
      "amount": {
        "currency_code": "USD",
        "value": "100.00"
      }
    }
  ],
  "paypal_order_id": "PP-123",
  "payer_id": "PAYER123"
}

The internal model must not know about vendor-specific field names.

ORCHESTRATOR
------------
Create PaymentOrchestrator.

Responsibilities:
- Generate payment ID.
- Create PaymentContext.
- Resolve the correct PaymentMethodHandler.
- Execute the payment flow.
- Return normalized PaymentResponse.

The orchestrator should not contain Card or PayPal-specific business logic.

CONTROLLER
----------
Create:

PaymentController

Endpoint:

POST /v1/payments

The controller should:
- Accept PaymentRequest.
- Perform common bean validation.
- Call PaymentOrchestrator.
- Return PaymentResponse.

Do NOT put payment-method-specific processing in the controller.

DATABASE
--------
Use Spring Data JPA and H2 for the reference implementation.

Create a Payment entity containing:

- paymentId
- merchantId
- orderId
- paymentMethod
- amount
- currency
- status
- gateway
- gatewayTransactionId
- createdAt
- updatedAt

Design the entity so it can later be migrated to PostgreSQL/DynamoDB.

IDEMPOTENCY
-----------
Design the application to support:

Idempotency-Key

The architecture should prevent duplicate payment processing when the same request is submitted multiple times.

Create an Idempotency record containing:

- idempotencyKey
- paymentId
- requestHash
- response
- status
- createdAt

For the reference implementation, implement the basic idempotency flow.

ERROR HANDLING
--------------
Create centralized exception handling.

Handle:

- Invalid payment method
- Missing payment-specific fields
- Invalid amount
- Gateway failure
- Gateway timeout
- Duplicate idempotency key
- Unexpected system errors

Return a consistent error response.

RESILIENCE
----------
Design the gateway layer so it can later support:

- Timeout
- Retry
- Circuit breaker
- Rate limiting

Do NOT blindly retry payment authorization because a timeout can happen after the gateway has successfully processed the payment.

Mention how gateway idempotency should be used.

OBSERVABILITY
-------------
Design support for:

- correlationId
- paymentId
- merchantId
- gatewayTransactionId
- idempotencyKey

Do not log:
- CVV
- full card number
- authentication tokens
- sensitive payment credentials

PACKAGE STRUCTURE
-----------------

Use:

com.example.payment

├── controller
│   ├── PaymentController
│   └── GlobalExceptionHandler
│
├── service
│   └── PaymentOrchestrator
│
├── strategy
│   ├── PaymentMethodHandler
│   ├── PaymentHandlerRegistry
│   ├── CardPaymentHandler
│   └── PaypalPaymentHandler
│
├── validation
│   ├── PaymentValidator
│   ├── ValidatorRegistry
│   ├── CardPaymentValidator
│   └── PaypalPaymentValidator
│
├── gateway
│   ├── PaymentGateway
│   ├── CardGatewayAdapter
│   └── PaypalGatewayAdapter
│
├── mapper
│   ├── CardGatewayMapper
│   └── PaypalGatewayMapper
│
├── model
│   ├── PaymentRequest
│   ├── PaymentResponse
│   ├── PaymentContext
│   ├── PaymentMethod
│   ├── PaymentStatus
│   ├── Money
│   ├── GatewayRequest
│   └── GatewayResponse
│
├── entity
│   └── PaymentEntity
│
├── repository
│   └── PaymentRepository
│
└── exception

TECHNOLOGY
----------
Use:

- Java 21
- Spring Boot 3.x
- Maven
- Spring Web
- Spring Validation
- Spring Data JPA
- H2
- SpringDoc OpenAPI / Swagger
- JUnit 5
- Mockito

CODING REQUIREMENTS
-------------------
- Use Java records where appropriate.
- Use constructor injection.
- Follow SOLID principles.
- Keep classes small and focused.
- Avoid unnecessary abstractions.
- Do not use giant if/else or switch statements for payment methods.
- Use interfaces where behavior varies.
- Use meaningful exception classes.
- Write clean production-quality Java.
- Include JavaDoc for important architectural interfaces.
- Use proper logging.
- Do not expose gateway-specific models outside the gateway layer.

TESTING
-------
Create:

1. Unit tests for CardPaymentHandler.
2. Unit tests for PaypalPaymentHandler.
3. Unit tests for CardPaymentValidator.
4. Unit tests for PaypalPaymentValidator.
5. Unit tests for gateway mappers.
6. Unit tests for PaymentHandlerRegistry.
7. Controller integration tests.
8. End-to-end tests for CARD.
9. End-to-end tests for PAYPAL.
10. Idempotency tests.

DOCUMENTATION
-------------
Create a README containing:

1. Business requirements.
2. Architecture overview.
3. HLD diagram.
4. LLD diagram.
5. End-to-end sequence diagram.
6. Package structure.
7. Design patterns used.
8. Why Strategy Pattern is used.
9. Why Adapter Pattern is used.
10. Why Mapper Pattern is used.
11. CARD flow.
12. PAYPAL flow.
13. Error handling.
14. Idempotency.
15. Retry/resilience strategy.
16. Database design.
17. How to add a new payment method.
18. How to replace mocked gateways with real APIs.
19. How to run the application.
20. Sample curl requests.
21. Swagger URL.

IMPORTANT ARCHITECTURAL REQUIREMENT
------------------------------------
The most important requirement is extensibility.

If a new payment method such as APPLE_PAY is added, the developer should primarily add:

- ApplePayPaymentHandler
- ApplePayValidator
- ApplePayGatewayMapper
- ApplePayGatewayAdapter

Existing Card and PayPal classes should NOT need modification.

Similarly, if PayPal changes its external JSON contract, only the PayPal mapper/gateway integration should need modification.

The internal canonical payment model must remain stable.

DELIVERABLE
-----------
Generate the complete Maven project with all source code, tests, configuration, README, diagrams, and sample requests.

The project must compile successfully with:

mvn clean test

and run with:

mvn spring-boot:run


Swagger-
http://localhost:8080/swagger-ui/index.html

