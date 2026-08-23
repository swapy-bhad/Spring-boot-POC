import { useEffect, useState } from "react";
import { fetchSupportedMethods, fetchFieldConfig, submitPayment } from "./api";

/**
 * Fully data-driven payment form. It never hard-codes "cardToken" or
 * "paypalOrderId" - it asks the backend which methods exist, asks for that
 * method's field definitions, and renders inputs generically from those
 * definitions. Adding a new payment method on the backend (new processor +
 * new field-config JSON file) makes it show up here automatically, with no
 * frontend code changes.
 */
export default function DynamicPaymentForm() {
  const [methods, setMethods] = useState([]);
  const [selectedMethod, setSelectedMethod] = useState("");
  const [fields, setFields] = useState([]);
  const [formValues, setFormValues] = useState({});
  const [result, setResult] = useState(null);
  const [error, setError] = useState(null);
  const [submitting, setSubmitting] = useState(false);

  useEffect(() => {
    fetchSupportedMethods()
      .then((data) => {
        const list = Array.isArray(data) ? data : Array.from(data);
        setMethods(list);
        if (list.length) setSelectedMethod(list[0]);
      })
      .catch((e) => setError(e.message));
  }, []);

  useEffect(() => {
    if (!selectedMethod) return;
    setFormValues({});
    setResult(null);
    setError(null);
    fetchFieldConfig(selectedMethod)
      .then(setFields)
      .catch((e) => setError(e.message));
  }, [selectedMethod]);

  const handleChange = (name, value) => {
    setFormValues((prev) => ({ ...prev, [name]: value }));
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError(null);
    setResult(null);
    setSubmitting(true);
    try {
      const response = await submitPayment(selectedMethod, formValues);
      setResult(response);
    } catch (err) {
      setError(err.message);
    } finally {
      setSubmitting(false);
    }
  };

  const renderField = (field) => {
    if (field.type === "select") {
      return (
        <select
          value={formValues[field.name] || ""}
          onChange={(e) => handleChange(field.name, e.target.value)}
          required={field.required}
        >
          <option value="">Select...</option>
          {(field.options || []).map((opt) => (
            <option key={opt} value={opt}>
              {opt}
            </option>
          ))}
        </select>
      );
    }
    return (
      <input
        type={field.type === "number" ? "number" : "text"}
        value={formValues[field.name] || ""}
        onChange={(e) => handleChange(field.name, e.target.value)}
        pattern={field.pattern || undefined}
        required={field.required}
      />
    );
  };

  return (
    <div style={{ maxWidth: 420, margin: "40px auto", fontFamily: "sans-serif" }}>
      <h2>Make a Payment</h2>

      <label style={{ display: "block", marginBottom: 4 }}>Payment Method</label>
      <select
        value={selectedMethod}
        onChange={(e) => setSelectedMethod(e.target.value)}
        style={{ marginBottom: 16 }}
      >
        {methods.map((m) => (
          <option key={m} value={m}>
            {m}
          </option>
        ))}
      </select>

      <form onSubmit={handleSubmit}>
        {fields.map((field) => (
          <div key={field.name} style={{ margin: "12px 0" }}>
            <label style={{ display: "block", marginBottom: 4 }}>
              {field.label}
              {field.required ? " *" : ""}
            </label>
            {renderField(field)}
          </div>
        ))}
        {fields.length > 0 && (
          <button type="submit" disabled={submitting}>
            {submitting ? "Processing..." : "Pay"}
          </button>
        )}
      </form>

      {result && (
        <p style={{ color: "green" }}>
          {result.status}: {result.transactionId}
        </p>
      )}
      {error && <p style={{ color: "red" }}>{error}</p>}
    </div>
  );
}
