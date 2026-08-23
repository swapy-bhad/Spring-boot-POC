# Payment Processing System — Architecture Notes

## The core problem

One API, wildly different request shapes and downstream logic per payment
method, and new methods need to slot in without touching existing code.
That's a textbook case for the **Strategy pattern** (you called it
"provider pattern" — same idea, common naming in payment systems: each
method is a `PaymentProvider`/`PaymentProcessor`), combined with a
**Template Method** for the steps every strategy repeats, and a thin
**Filter** for the cross-cutting header parsing.

## Request flow

```
Client
  │  POST /api/payments
  │  Header: X-Payment-Method: CARD
  │  Body: { cardToken, expiryMonth, expiryYear, amount, currency }
  ▼
PaymentMethodHeaderFilter        (reads header, rejects unknown/missing method,
                                   stores it in a request-scoped ThreadLocal)
  ▼
PaymentController                (generic — reads the method from context,
                                   asks the registry to resolve a processor,
                                   hands it the raw JSON body)
  ▼
PaymentProcessorRegistry         (Map<String, PaymentProcessor> built at
                                   startup from every PaymentProcessor bean
                                   Spring finds — no manual registration)
  ▼
CardPaymentProcessor / PaypalPaymentProcessor   (implements PaymentProcessor,
                                   extends AbstractPaymentProcessor)
      1. parseRequest   — JSON → this method's own typed DTO
      2. validate        — Bean Validation on that DTO
      3. toGatewayRequest — map to the external gateway's own contract
      4. callGateway      — Adapter that calls the real gateway/API
      5. toPaymentResult  — map gateway's response → generic PaymentResult
  ▼
Generic PaymentResult { transactionId, status, message } → client
```

## Why this satisfies "add a method without touching existing code"

- **`PaymentProcessorRegistry`** takes `List<PaymentProcessor>` in its
  constructor. Spring auto-collects every bean implementing that interface.
  There's no `switch`/`if` on a method-type enum anywhere — the set of
  supported methods *is* the set of `@Component`-annotated processors.
- **`PaymentController`** only knows `PaymentProcessor` and `JsonNode`. It
  never sees `cardToken` or `paypalOrderId`.
- **`AbstractPaymentProcessor`** gives you the orchestration for free
  (Template Method) so a new processor is ~5 small methods, not a
  copy-pasted parse/validate/call/map block.
- Each method's request/response DTOs, and its gateway client (an
  **Adapter** translating your domain model to that gateway's actual JSON
  contract), live in their own package and don't leak into anyone else's.

### Adding a new method, e.g. APPLEPAY

1. `dto/applepay/ApplePayPaymentRequest.java` (+ gateway req/resp DTOs) —
   whatever fields Apple Pay actually needs.
2. `gateway/ApplePayGatewayClient.java` — calls the real API.
3. `processor/ApplePayPaymentProcessor.java extends AbstractPaymentProcessor<...>`,
   annotated `@Component`, `getMethodType()` returns `"APPLEPAY"`.
4. `resources/field-config/applepay.json` — field definitions for the UI.

Nothing in `PaymentController`, `PaymentProcessorRegistry`,
`PaymentMethodHeaderFilter`, `CardPaymentProcessor`, or
`PaypalPaymentProcessor` changes. The frontend picks it up automatically
(see below).

## Data-driven, configurable UI

`PaymentMethodConfigController` exposes:

- `GET /api/payment-methods` → `["CARD", "PAYPAL"]`
- `GET /api/payment-methods/{method}/fields` → a list of `FieldDefinition`
  (`name`, `label`, `type`, `required`, `pattern`, `options`), loaded from
  `resources/field-config/<method>.json`.

`DynamicPaymentForm.jsx` fetches the method list, fetches that method's
field list, and renders inputs generically (`text` / `number` / `select`)
from the definitions. It never hard-codes a field name. Result: step 4
above ("drop in a JSON file") is *also* the entire frontend change needed
for a new method.

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

## Deliberate trade-off worth knowing about

The controller accepts the body as `JsonNode` rather than a typed DTO,
because it genuinely can't know the shape ahead of time — that's the whole
point. You lose OpenAPI-level type safety at the controller boundary in
exchange for real method-agnosticism. Each processor still gets full
compile-time type safety and Bean Validation internally, right after
`parseRequest`. If you want the boundary typed too, an alternative is
Jackson polymorphic deserialization (`@JsonTypeInfo`/`@JsonSubTypes`) keyed
off a `type` field in the body instead of the header — works, but couples
the body schema to a discriminator field and is harder to keep in sync with
a header-driven routing requirement like yours.

## Running it

Backend: `cd backend && mvn spring-boot:run` (Java 21, Spring Boot 3.3).
Frontend: `cd frontend && npm install && npm run dev`.

Gateway clients (`CardGatewayClient`, `PaypalGatewayClient`) are stubbed to
simulate a successful charge/capture — swap their internals for real
`RestClient`/`WebClient` calls to the actual Card/PayPal APIs; nothing else
in the flow needs to change.
