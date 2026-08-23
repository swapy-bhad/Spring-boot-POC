const API_BASE = "http://localhost:8080/api";

export async function fetchSupportedMethods() {
  const res = await fetch(`${API_BASE}/payment-methods`);
  if (!res.ok) throw new Error("Failed to load payment methods");
  return res.json();
}

export async function fetchFieldConfig(method) {
  const res = await fetch(`${API_BASE}/payment-methods/${method}/fields`);
  if (!res.ok) throw new Error(`Failed to load fields for ${method}`);
  return res.json();
}

export async function submitPayment(method, values) {
  // "type" is the Jackson polymorphic discriminator the backend's
  // PaymentRequest hierarchy dispatches on - it has to be in the body
  // itself, not just the header, for @RequestBody PaymentRequest to
  // resolve to the right concrete class (CardPaymentRequest, etc).
  const res = await fetch(`${API_BASE}/payments`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      "X-Payment-Method": method,
    },
    body: JSON.stringify({ type: method, ...values }),
  });
  const body = await res.json();
  if (!res.ok) {
    throw new Error(body.message || "Payment failed");
  }
  return body;
}
