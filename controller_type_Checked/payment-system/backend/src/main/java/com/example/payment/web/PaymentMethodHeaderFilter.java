package com.example.payment.web;

import com.example.payment.domain.PaymentMethodContext;
import com.example.payment.processor.PaymentProcessorRegistry;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;

/**
 * Extracts and validates the payment method identifier from the
 * X-Payment-Method request header before the request ever reaches the
 * controller. This is the ONE place that knows the identifier travels via a
 * header - if it later moved to a JWT claim or a path segment, only this
 * class would change.
 *
 * Unknown/missing methods are rejected here with a 400, so the generic
 * controller and every processor can assume the method is always valid.
 */
@Component
public class PaymentMethodHeaderFilter extends OncePerRequestFilter {

    private static final String HEADER_NAME = "X-Payment-Method";
    private static final ObjectMapper JSON = new ObjectMapper();

    private final PaymentProcessorRegistry registry;

    public PaymentMethodHeaderFilter(PaymentProcessorRegistry registry) {
        this.registry = registry;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        if (isPaymentEndpoint(request)) {
            String methodType = request.getHeader(HEADER_NAME);

            if (methodType == null || methodType.isBlank()) {
                writeError(response, HttpServletResponse.SC_BAD_REQUEST,
                        "MISSING_HEADER", "Required header '" + HEADER_NAME + "' was not supplied");
                return;
            }
            if (!registry.isSupported(methodType)) {
                writeError(response, HttpServletResponse.SC_BAD_REQUEST,
                        "UNSUPPORTED_PAYMENT_METHOD", "Unsupported payment method: " + methodType);
                return;
            }
            PaymentMethodContext.set(methodType.toUpperCase());
        }

        try {
            chain.doFilter(request, response);
        } finally {
            PaymentMethodContext.clear();
        }
    }

    private boolean isPaymentEndpoint(HttpServletRequest request) {
        return request.getRequestURI().startsWith("/api/payments") && "POST".equalsIgnoreCase(request.getMethod());
    }

    private void writeError(HttpServletResponse response, int status, String error, String message) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json");
        JSON.writeValue(response.getWriter(), Map.of("error", error, "message", message));
    }
}
