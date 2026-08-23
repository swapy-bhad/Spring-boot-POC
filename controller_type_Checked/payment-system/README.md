# Payment Processing System — Architecture Notes

## The core problem

One API, wildly different request shapes and downstream logic per payment
method, and new methods need to slot in without touching existing code.
That's a textbook case for the **Strategy pattern** (you called it
"provider pattern" — same idea, common naming in payment systems: each
method is a `PaymentProvider`/`PaymentProcessor`), combined with a
**Template Method** for the steps every strategy repeats, and a thin
**Filter** for the cross-cutting header parsing.

The controller binds a genuinely typed request body via **Jackson
polymorphic deserialization**, rather than accepting raw `JsonNode` — see
[Why a typed controller](#why-a-typed-controller) for the trade-off this
buys you and the one thing it costs.

## Request flow

```
Client
  │  POST /api/payments
  │  Header: X-Payment-Method: CARD
  │  Body: { "type": "CARD", cardToken, expiryMonth, expiryYear, amount, currency }
  ▼
PaymentMethodHeaderFilter        (reads header, rejects unknown/missing method,
                                   stores it in a request-scoped ThreadLocal —
                                   cheap rejection before the body is even parsed)
  ▼
Spring's HttpMessageConverter    (Jackson reads the body's own "type" field and
                                   deserializes straight into CardPaymentRequest —
                                   see PaymentRequest's @JsonTypeInfo/@JsonSubTypes —
                                   then runs Bean Validation via @Valid)
  ▼
PaymentController                (generic — takes the already-typed, already-
                                   validated PaymentRequest; cross-checks the
                                   header against the body's actual type;
                                   asks the registry to dispatch it)
  ▼
PaymentProcessorRegistry         (Map<Class<?>, PaymentProcessor<?>> AND
                                   Map<String, PaymentProcessor<?>>, both built
                                   at startup from every PaymentProcessor bean
                                   Spring finds — no manual registration)
  ▼
CardPaymentProcessor / PaypalPaymentProcessor   (implements PaymentProcessor<T>,
                                   extends AbstractPaymentProcessor)
      1. toGatewayRequest — map the typed request to the external gateway's own contract
      2. callGateway      — Adapter that calls the real gateway/API
      3. toPaymentResult  — map gateway's response → generic PaymentResult
  ▼
Generic PaymentResult { transactionId, status, message } → client
```

## Why a typed controller

Originally the controller took the body as `JsonNode`, because a single
method-agnostic endpoint genuinely can't know the shape ahead of time. That
sacrifices compile-time and OpenAPI-level type safety at the boundary in
exchange for zero coupling to any one method's fields.

Swapping to **Jackson polymorphic deserialization** gets the type safety
back:

- `PaymentRequest` is a plain marker interface annotated with
  `@JsonTypeInfo(use = NAME, include = PROPERTY, property = "type")` and
  `@JsonSubTypes({...})` mapping `"CARD"` → `CardPaymentRequest`, `"PAYPAL"`
  → `PaypalPaymentRequest`.
- The controller signature is now `processPayment(@Valid @RequestBody
  PaymentRequest request)` — a real type, not `JsonNode`. Jackson picks the
  concrete class from the body's own `"type"` field before the controller
  method even runs, and `@Valid` runs Bean Validation on whichever concrete
  type it resolved to.
- `AbstractPaymentProcessor` shrank from five template steps to three
  (`toGatewayRequest` → `callGateway` → `toPaymentResult`) — parsing and
  validation aren't the processor's job anymore, the framework boundary
  already did both.
- `PaymentProcessorRegistry` now also keeps a `Class<?> →
  PaymentProcessor<?>` map, built the same auto-collecting way as before, so
  it can dispatch an already-typed request to the right processor with an
  unchecked cast in one place instead of an `instanceof` chain anywhere
  else.

**The one thing this costs:** the JsonNode design needed *zero* shared-file
edits to add a method. This design needs exactly one — registering the new
subtype in `PaymentRequest`'s `@JsonSubTypes` list, because that's the only
place Jackson can learn `"APPLEPAY"` maps to `ApplePayPaymentRequest`.
`PaymentRequest` is deliberately a plain interface, not `sealed`, specifically
to avoid a *second* shared edit (a `permits` clause) — Java's exhaustiveness
checking is nice, but not nice enough to justify touching shared code twice
per method. Everything downstream of that one registration —
`PaymentController`, `PaymentProcessorRegistry`, `PaymentMethodHeaderFilter`,
`CardPaymentProcessor`, `PaypalPaymentProcessor` — is still untouched.

**The header didn't go away.** `PaymentMethodHeaderFilter` still validates
`X-Payment-Method` before the body is parsed at all (cheap fail-fast, and
still the mechanism your original spec asked for), and the controller now
cross-checks it against what the body actually deserialized to — a caller
sending a CARD body under a PAYPAL header gets a clear `422` instead of a
confusing downstream mismatch.

## Why this still satisfies "add a method without touching existing code"

(modulo the one `@JsonSubTypes` line above)

- **`PaymentProcessorRegistry`** takes `List<PaymentProcessor<?>>` in its
  constructor. Spring auto-collects every bean implementing that interface.
  There's no `switch`/`if` on a method-type enum anywhere — the set of
  supported methods *is* the set of `@Component`-annotated processors.
- **`AbstractPaymentProcessor`** gives you the orchestration for free
  (Template Method) so a new processor is 3 small methods, not a
  copy-pasted map/call/map block.
- Each method's request/response DTOs, and its gateway client (an
  **Adapter** translating your domain model to that gateway's actual JSON
  contract), live in their own package and don't leak into anyone else's.

### Adding a new method, e.g. APPLEPAY

1. `PaymentRequest.java` — add one line to `@JsonSubTypes`:
   `@JsonSubTypes.Type(value = ApplePayPaymentRequest.class, name = "APPLEPAY")`.
   (The one shared-code edit this design costs.)
2. `dto/applepay/ApplePayPaymentRequest.java implements PaymentRequest` (+
   gateway req/resp DTOs) — whatever fields Apple Pay actually needs.
3. `gateway/ApplePayGatewayClient.java` — calls the real API.
4. `processor/ApplePayPaymentProcessor.java extends AbstractPaymentProcessor<...>`,
   annotated `@Component`, `getMethodType()` returns `"APPLEPAY"`,
   `getSupportedRequestType()` returns `ApplePayPaymentRequest.class`.
5. `resources/field-config/applepay.json` — field definitions for the UI.

Nothing in `PaymentController`, `PaymentProcessorRegistry`,
`PaymentMethodHeaderFilter`, `CardPaymentProcessor`, or
`PaypalPaymentProcessor` changes. The frontend picks it up automatically
(see below) — it just needs to send `{"type": "APPLEPAY", ...}` alongside
the matching header, same as it already does for CARD/PAYPAL.

## Data-driven, configurable UI

`PaymentMethodConfigController` exposes:

- `GET /api/payment-methods` → `["CARD", "PAYPAL"]`
- `GET /api/payment-methods/{method}/fields` → a list of `FieldDefinition`
  (`name`, `label`, `type`, `required`, `pattern`, `options`), loaded from
  `resources/field-config/<method>.json`.

`DynamicPaymentForm.jsx` fetches the method list, fetches that method's
field list, and renders inputs generically (`text` / `number` / `select`)
from the definitions. It never hard-codes a field name, and adds the `type`
discriminator itself when it submits (`{ type: method, ...values }`), so
step 5 above ("drop in a JSON file") is still effectively the entire
frontend-visible change for a new method.

## Fields as data — how far to take it

The sample loads field config from classpath JSON, which is enough to
answer "can fields be configurable" — yes, and it already decouples field
definitions from Java code. Two natural next steps if you want more:

- **DB-backed config** (a `payment_method_fields` table) instead of JSON
  files, so ops can add/edit fields without a redeploy. Only
  `PaymentMethodFieldConfigService` would change — the controller and
  frontend contract stay identical.
- **JSON Schema** instead of the current ad-hoc `FieldDefinition` shape, if
  you want richer validation (min/max, conditional fields, nested objects)
  and want to use an off-the-shelf renderer like `react-jsonschema-form` on
  the frontend instead of the hand-rolled one here.

## Running it

Backend: `cd backend && mvn spring-boot:run` (Java 21, Spring Boot 3.3).
Frontend: `cd frontend && npm install && npm run dev`.

Gateway clients (`CardGatewayClient`, `PaypalGatewayClient`) are stubbed to
simulate a successful charge/capture — swap their internals for real
`RestClient`/`WebClient` calls to the actual Card/PayPal APIs; nothing else
in the flow needs to change.

### Example requests

```bash
curl -X POST http://localhost:8080/api/payments \
  -H "Content-Type: application/json" \
  -H "X-Payment-Method: CARD" \
  -d '{"type":"CARD","cardToken":"tok_123","expiryMonth":"09","expiryYear":"2027","amount":42.50,"currency":"USD"}'

curl -X POST http://localhost:8080/api/payments \
  -H "Content-Type: application/json" \
  -H "X-Payment-Method: PAYPAL" \
  -d '{"type":"PAYPAL","paypalOrderId":"ORDER-1","payerId":"PAYER-1","amount":42.50,"currency":"USD"}'
```


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





                    <<interface>>
                 PaymentMethodHandler
                         ▲
              ┌──────────┴──────────┐
              │                     │
      CardPaymentHandler     PaypalPaymentHandler
              │                     │
              │                     │
      CardPaymentValidator   PaypalPaymentValidator
              │                     │
              ▼                     ▼
       CardGatewayMapper      PaypalGatewayMapper
              │                     │
              ▼                     ▼
       CardGatewayAdapter      PaypalGatewayAdapter
              │                     │
              └──────────┬──────────┘
                         │
                  <<interface>>
                  PaymentGateway


PaymentRequest
│
▼
PaymentContext
│
├── PaymentMethod
├── Money
├── Merchant
├── Order
└── PaymentDetails
│
┌─────┴─────┐
│           │
Card        PayPal
Details       Details



E-2-E
Client
│
│ POST /payments
▼
Controller
│
├── Validate common DTO
│
▼
Idempotency Service
│
├── Check key
│
▼
Payment Orchestrator
│
├── Create Payment ID
├── Persist CREATED
│
▼
Handler Registry
│
├── CARD
▼
Card Handler
│
├── Validate Card Details
├── Build PaymentContext
▼
Card Mapper
│
├── Internal DTO → Gateway DTO
▼
Card Gateway Adapter
│
├── Add auth headers
├── Add gateway idempotency key
├── HTTP call
▼
External Card Gateway
│
├── AUTH
▼
Response
│
▼
Gateway Adapter
│
├── Vendor response → GatewayResponse
▼
Payment Orchestrator
│
├── Update state
├── Persist transaction
├── Publish event
▼
PaymentResponse
│
▼
Client
