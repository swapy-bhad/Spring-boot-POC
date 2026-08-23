If the controller endpoint/action is the same, but the payment method is different, you generally should not create separate controllers. Instead, pass the payment method in the request and use a Strategy Pattern in the service layer.

Example

Same API:
POST /payments

{
  "amount": 100,
  "paymentMethod": "CARD"
}

{
  "amount": 100,
  "paymentMethod": "PAYPAL"
}

{
  "amount": 100,
  "paymentMethod": "APPLE_PAY"
}


Controller stays the same - The controller doesn’t care which payment method it is.

@PostMapping("/payments")
public PaymentResponse makePayment(
        @RequestBody PaymentRequest request) {

    return paymentService.processPayment(request);
}


Service differentiates the payment method

Instead of doing this:
if (request.getPaymentMethod().equals("CARD")) {
    // card logic
} else if (request.getPaymentMethod().equals("PAYPAL")) {
    // paypal logic
} else if (...) {
    // ...
}

Use a strategy/Provider interface:

public interface PaymentProcessor {
    PaymentResponse process(PaymentRequest request);
}

Implement it for each payment type:
@Component("CARD")
public class CardPaymentProcessor implements PaymentProcessor {

    public PaymentResponse process(PaymentRequest request) {
        // Card processing
    }
}



@Component("PAYPAL")
public class PaypalPaymentProcessor implements PaymentProcessor {

    public PaymentResponse process(PaymentRequest request) {
        // PayPal processing
    }
}
Then select the correct implementation

Spring approach
@Service
public class PaymentService {

    private final Map<String, PaymentProcessor> processors;

    public PaymentService(List<PaymentProcessor> processors) {
        this.processors = processors.stream()
                .collect(Collectors.toMap(
                    p -> p.getClass()
                          .getAnnotation(Component.class)
                          .value(),
                    Function.identity()
                ));
    }

    public PaymentResponse processPayment(PaymentRequest request) {

        PaymentProcessor processor =
                processors.get(request.getPaymentMethod());

        if (processor == null) {
            throw new IllegalArgumentException("Unsupported payment method");
        }

        return processor.process(request);
    }
}

A cleaner production design is to have the strategy explicitly expose its type:
public interface PaymentProcessor {

    PaymentMethod getPaymentMethod();

    PaymentResponse process(PaymentRequest request);

}

@Component
public class CardPaymentProcessor implements PaymentProcessor {

    @Override
    public PaymentMethod getPaymentMethod() {
        return PaymentMethod.CARD;
    }

    @Override
    public PaymentResponse process(PaymentRequest request) {
        // card logic
    }
}

@Component
public class PaypalPaymentProcessor implements PaymentProcessor {

    @Override
    public PaymentMethod getPaymentMethod() {
        return PaymentMethod.PAYPAL;
    }

    @Override
    public PaymentResponse process(PaymentRequest request) {
        // PayPal logic
    }
}


***

I would design it as a canonical payment API + payment-method-specific adapters/strategies + gateway adapters.

                         ┌─────────────────────┐
                         │       Client        │
                         └──────────┬──────────┘
                                    │
                                    │ POST /payments
                                    ▼
                         ┌─────────────────────┐
                         │ Payment Controller  │
                         └──────────┬──────────┘
                                    │
                                    ▼
                         ┌─────────────────────┐
                         │ Payment Orchestrator│
                         └──────────┬──────────┘
                                    │
                         paymentMethod?
                    ┌───────────────┼────────────────┐
                    │               │                │
                    ▼               ▼                ▼
             ┌────────────┐  ┌────────────┐  ┌────────────┐
             │    CARD    │  │   PAYPAL   │  │  FUTURE    │
             │  Strategy  │  │  Strategy  │  │  Strategy  │
             └─────┬──────┘  └─────┬──────┘  └────────────┘
                   │               │
                   ▼               ▼
             ┌────────────┐  ┌────────────┐
             │Card Mapper │  │PayPal Mapper│
             └─────┬──────┘  └─────┬──────┘
                   │               │
                   ▼               ▼
             ┌────────────┐  ┌────────────┐
             │ Card       │  │ PayPal     │
             │ Gateway    │  │ Gateway    │
             │ Adapter    │  │ Adapter     │
             └─────┬──────┘  └─────┬──────┘
                   │               │
                   ▼               ▼
             Card Network      PayPal API

2. The most important design decision

Don’t do this

if (paymentMethod == CARD) {
   // 500 lines
} else if (paymentMethod == PAYPAL) {
   // 500 lines
}


And don’t create one giant:

PaymentRequest. -->. That becomes unmaintainable.

Instead:
Common Payment Information
+
Payment-specific payload


3. API request

I would expose a canonical API such as:

POST /v1/payments

Request:PAYPAL
{
  "paymentMethod": "PAYPAL",
  "amount": {
    "value": 100.00,
    "currency": "USD"
  },
  "merchant": {
    "merchantId": "M123"
  },
  "order": {
    "orderId": "ORD123"
  },
  "paymentDetails": {
    "paypalOrderId": "PAYPAL-123",
    "payerId": "PAYER123"
  }
}

Request: CARD
{
  "paymentMethod": "CARD",
  "amount": {
    "value": 100.00,
    "currency": "USD"
  },
  "merchant": {
    "merchantId": "M123"
  },
  "order": {
    "orderId": "ORD123"
  },
  "paymentDetails": {
    "cardToken": "tok_123",
    "expiryMonth": 12,
    "expiryYear": 2029
  }
}


4. Java object model

I would use: common param

public class PaymentRequest {

    private PaymentMethod paymentMethod;

    private Money amount;

    private Merchant merchant;

    private Order order;

    private JsonNode paymentDetails;
}

Then: Specific params seperate class
public enum PaymentMethod {
    CARD,
    PAYPAL,
    FUTURE_PAYMENT_METHOD
}

5. Strategy layer

Create a common interface:

public interface PaymentMethodHandler {

    PaymentMethod supportedMethod();

    PaymentResult process(PaymentContext context);
}

implementation
PaymentMethodHandler
        |
        +---- CardPaymentHandler
        |
        +---- PaypalPaymentHandler
        |
        +---- BankPaymentHandler
        |
        +---- FuturePaymentHandler

6. Payment Orchestrator

This becomes the central coordinator.

@Service
public class PaymentOrchestrator {

    private final Map<PaymentMethod, PaymentMethodHandler> handlers;

    public PaymentOrchestrator(
            List<PaymentMethodHandler> handlerList) {

        handlers = handlerList.stream()
                .collect(Collectors.toMap(
                        PaymentMethodHandler::supportedMethod,
                        Function.identity()
                ));
    }

    public PaymentResult process(PaymentRequest request) {

        PaymentMethodHandler handler =
                handlers.get(request.getPaymentMethod());

        if (handler == null) {
            throw new UnsupportedPaymentMethodException();
        }

        PaymentContext context =
                PaymentContext.from(request);

        return handler.process(context);
    }
}


Controller remains extremely simple:

@RestController
public class PaymentController {

    @PostMapping("/v1/payments")
    public PaymentResponse createPayment(
            @RequestBody PaymentRequest request) {

        return paymentOrchestrator.process(request);
    }
}

7. But your gateway problem is important

You said:

PayPal → PayPal APIs
Card → Card gateway
Infrastructure → different gateway

This is where I would introduce a Gateway Adapter layer.

Don’t let your payment strategy directly implement every vendor’s API.

Instead:

PaymentMethodHandler
        |
        ↓
Payment Gateway Interface
        |
   ┌────┼──────────────┐
   ↓    ↓              ↓
Card   PayPal      Infrastructure
Gateway Gateway       Gateway
Adapter  Adapter       Adapter
   ↓      ↓              ↓
Card     PayPal       Internal
API       API          Gateway


8. Gateway interface

public interface PaymentGateway {

    GatewayResponse authorize(
            GatewayRequest request);

    GatewayResponse capture(
            GatewayRequest request);

    GatewayResponse refund(
            GatewayRequest request);
}

Then:

@Component
public class CardGatewayAdapter
        implements PaymentGateway {

    @Override
    public GatewayResponse authorize(
            GatewayRequest request) {

        // Convert internal request
        // to Card gateway JSON

        // Call Card API

        // Convert Card response
        // to GatewayResponse
    }
}


PayPal:
@Component
public class PaypalGatewayAdapter
        implements PaymentGateway {

    @Override
    public GatewayResponse authorize(
            GatewayRequest request) {

        // Convert internal request
        // to PayPal JSON

        // Call PayPal

        // Normalize response
    }
}


9. Now you have two levels of abstraction

This is the important part.

Level 1 — Payment method

CARD
PAYPAL
BANK

Level 2 — Gateway
CARD → Card Gateway
PAYPAL → PayPal
BANK → Internal Gateway


                 Payment
                    │
                    ▼
           PaymentMethodHandler
                    │
        ┌───────────┼────────────┐
        │           │            │
       CARD       PAYPAL       BANK
        │           │            │
        ▼           ▼            ▼
    CardGateway  PaypalGateway  BankGateway
        │           │            │
        ▼           ▼            ▼
     Card API    PayPal API   Internal API



10. Mapping is extremely important

Don’t send your internal Java objects directly to external APIs.

For example, your internal model:

PaymentContext

might contain:
paymentId
merchantId
amount
currency
paymentMethod
paymentDetails

But PayPal may require:

{
  "intent": "CAPTURE",
  "purchase_units": [
    {
      "amount": {
        "currency_code": "USD",
        "value": "100.00"
      }
    }
  ]
}

Don’t make your core application understand those formats.


Instead:
Internal Model
     │
     ▼
Gateway Mapper
     │
     ├──────→ PayPal Request
     │
     ├──────→ Card Request
     │
     └──────→ Infrastructure Request

11. Complete E2E flow

Here’s the flow I’d propose for your system:

Client
  │
  │ POST /v1/payments
  ▼
┌──────────────────────┐
│ Payment Controller   │
│                      │
│ Validate request     │
└──────────┬───────────┘
           │
           ▼
┌──────────────────────┐
│ Payment Orchestrator │
└──────────┬───────────┘
           │
           │ paymentMethod
           ▼
┌──────────────────────┐
│ Handler Factory      │
└──────────┬───────────┘
           │
     ┌─────┴─────┐
     │           │
     ▼           ▼
  CardHandler  PaypalHandler
     │           │
     ▼           ▼
Card Mapper   Paypal Mapper
     │           │
     ▼           ▼
Card Gateway  PayPal Gateway
     │           │
     ▼           ▼
 Card API      PayPal API
     │           │
     └─────┬─────┘
           │
           ▼
┌──────────────────────┐
│ Response Mapper      │
└──────────┬───────────┘
           │
           ▼
┌──────────────────────┐
│ PaymentResponse      │
└──────────┬───────────┘
           │
           ▼
        Client


12. Where validation happens

This is another important LLD decision.

You have common validation:
amount required
currency required
merchantId required
paymentMethod required


And payment-specific validation:
CARD
 ├── cardToken required
 └── expiry required

PAYPAL
 ├── paypalOrderId required
 └── payerId required


                     Request
                       │
                       ▼
              Common Validation
                       │
                       ▼
              Payment Method
                   Validation
                  /         \
               CARD        PAYPAL
                │             │
          CardValidator   PaypalValidator


15. Idempotency

For a payment system, this is mandatory.

POST /payments
Idempotency-Key: abc-123
If the same request comes again:

Request 1
    ↓
Payment created
    ↓
Gateway called
    ↓
SUCCESS

Request 2
same idempotency key
    ↓
Don't call gateway again
    ↓
Return previous result


Database: 
IDEMPOTENCY
--------------------
idempotency_key
payment_id
request_hash
response
status
created_at


                 ┌─────────────────────┐
                 │       CLIENT        │
                 └──────────┬──────────┘
                            │
                            ▼
                 ┌─────────────────────┐
                 │ PAYMENT CONTROLLER  │
                 └──────────┬──────────┘
                            │
                            ▼
                 ┌─────────────────────┐
                 │ PAYMENT ORCHESTRATOR│
                 └──────────┬──────────┘
                            │
                    Payment Method
                            │
             ┌──────────────┼──────────────┐
             ▼              ▼              ▼
        ┌─────────┐    ┌─────────┐    ┌─────────┐
        │  CARD   │    │ PAYPAL  │    │  BANK   │
        │ HANDLER │    │ HANDLER │    │ HANDLER │
        └────┬────┘    └────┬────┘    └────┬────┘
             │              │              │
             ▼              ▼              ▼
        ┌─────────┐    ┌─────────┐    ┌─────────┐
        │  CARD   │    │ PAYPAL  │    │  BANK   │
        │  MAPPER │    │  MAPPER │    │  MAPPER │
        └────┬────┘    └────┬────┘    └────┬────┘
             │              │              │
             ▼              ▼              ▼
        ┌─────────┐    ┌─────────┐    ┌─────────┐
        │  CARD   │    │ PAYPAL  │    │  BANK   │
        │ GATEWAY │    │ GATEWAY │    │ GATEWAY │
        └────┬────┘    └────┬────┘    └────┬────┘
             │              │              │
             ▼              ▼              ▼
          CARD API       PAYPAL API      BANK API

    
    The key design principles

Controller
→ Handles HTTP only.

Orchestrator
→ Coordinates the overall payment flow.

Strategy/Handler
→ Knows the business flow for a particular payment method.

Validator
→ Knows what that payment method requires.

Mapper
→ Converts your canonical/internal model into the vendor’s completely different JSON.

Gateway Adapter
→ Knows how to communicate with that specific external system.

Repository
→ Persists payment state.

This gives you a system where adding APPLE_PAY doesn’t require changing your existing Card or PayPal implementation:


Add:
    ApplePayHandler
    ApplePayValidator
    ApplePayMapper
    ApplePayGatewayAdapter

Existing:
    Card → unchanged
    PayPal → unchanged

Client
  │
  ▼
PaymentController
  │
  ▼
PaymentOrchestrator
  │
  │ paymentMethod = PAYPAL
  ▼
PaypalPaymentHandler
  │
  ├──────────────► PaypalValidator
  │                      │
  │                      ▼
  │                   Valid?
  │
  ▼
PaypalGatewayMapper
  │
  │ Internal → PayPal JSON
  ▼
PaypalGatewayAdapter
  │
  ▼
PayPal API
  │
  ▼
PayPal Response
  │
  ▼
Normalize Response
  │
  ▼
PaymentResponse



8. Sequence Diagram — Recommended for Presentation

Client       Controller    Orchestrator    Handler      Mapper      Gateway       External
  │              │              │             │           │            │             │
  │ POST /payment│              │             │           │            │             │
  ├─────────────►│              │             │           │            │             │
  │              │ process()    │             │           │            │             │
  │              ├─────────────►│             │           │            │             │
  │              │              │ resolve()   │           │            │             │
  │              │              ├────────────►│           │            │             │
  │              │              │             │ validate  │            │             │
  │              │              │             ├───────────┤            │             │
  │              │              │             │           │ map()      │             │
  │              │              │             ├──────────►│            │             │
  │              │              │             │           │            │             │
  │              │              │             │ gateway.authorize()     │             │
  │              │              │             ├───────────────────────►│             │
  │              │              │             │                        │ API call    │
  │              │              │             │                        ├────────────►│
  │              │              │             │                        │◄────────────┤
  │              │              │             │◄───────────────────────┤             │
  │              │              │◄────────────┤                          │             │
  │              │◄─────────────┤             │                          │             │
  │◄─────────────┤              │             │                          │             │


Gateway Adapter Architecture

                           Payment Handler
                               │
                               ▼
                      PaymentGateway
                         <<interface>>
                               │
             ┌─────────────────┼─────────────────┐
             │                 │                 │
             ▼                 ▼                 ▼
      CardGatewayAdapter  PaypalGatewayAdapter  BankGatewayAdapter
             │                 │                 │
             ▼                 ▼                 ▼
        Card REST API      PayPal REST API     Bank API


4. Different JSON Structures

This directly addresses your original problem.

                    Canonical Payment Request
                             │
                             ▼
                  ┌─────────────────────┐
                  │ PaymentContext      │
                  │                     │
                  │ amount              │
                  │ merchant            │
                  │ order               │
                  │ paymentMethod       │
                  │ paymentDetails      │
                  └──────────┬──────────┘
                             │
               ┌─────────────┴─────────────┐
               │                           │
              CARD                       PAYPAL
               │                           │
               ▼                           ▼
       ┌──────────────┐            ┌──────────────┐
       │ Card Mapper  │            │PayPal Mapper │
       └──────┬───────┘            └──────┬───────┘
              │                           │
              ▼                           ▼
       Card JSON                    PayPal JSON




Overall LLD Architecture-- > The controller and orchestration layer remain common, while payment-specific behavior is isolated using Strategy and Adapter patterns.
                         ┌──────────────────────┐
                         │       CLIENT         │
                         └──────────┬───────────┘
                                    │
                              POST /payments
                                    │
                                    ▼
                    ┌───────────────────────────┐
                    │    Payment Controller     │
                    │                           │
                    │  HTTP / Validation        │
                    └────────────┬──────────────┘
                                 │
                                 ▼
                    ┌───────────────────────────┐
                    │   Payment Orchestrator     │
                    │                           │
                    │  Common Payment Flow      │
                    └────────────┬──────────────┘
                                 │
                       Payment Method
                                 │
              ┌──────────────────┼──────────────────┐
              │                  │                  │
              ▼                  ▼                  ▼
       ┌─────────────┐    ┌─────────────┐    ┌─────────────┐
       │    CARD     │    │   PAYPAL    │    │   FUTURE    │
       │   Handler   │    │   Handler   │    │   Handler   │
       └──────┬──────┘    └──────┬──────┘    └─────────────┘
              │                  │
              ▼                  ▼
       ┌─────────────┐    ┌─────────────┐
       │ Card Mapper │    │PayPal Mapper│
       └──────┬──────┘    └──────┬──────┘
              │                  │
              ▼                  ▼
       ┌─────────────┐    ┌─────────────┐
       │Card Gateway │    │PayPal Gateway│
       │   Adapter   │    │   Adapter    │
       └──────┬──────┘    └──────┬──────┘
              │                  │
              ▼                  ▼
       ┌─────────────┐    ┌─────────────┐
       │   Card API  │    │  PayPal API │
       └─────────────┘    └─────────────┘

