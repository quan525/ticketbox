package com.example.ticketbox.payment.sepay;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Component
public class SepayDeliveryLog extends OncePerRequestFilter {
    private static final Logger LOG = LoggerFactory.getLogger(SepayDeliveryLog.class);
    public static final String PAYMENT_PROCESS_MS = SepayDeliveryLog.class.getName() + ".paymentProcessMs";
    private static final int MAX_DELIVERIES = 100;
    private final Deque<Delivery> deliveries = new ArrayDeque<>();
    private long sequence;

    public record Delivery(long id, Instant receivedAt, Instant finishedAt, int status, long durationMs, Long paymentProcessMs, String result) {}

    public synchronized List<Delivery> recent() {
        return List.copyOf(deliveries);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !"POST".equals(request.getMethod())
            || !request.getRequestURI().equals(request.getContextPath() + "/payments/sepay/webhook");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        Instant receivedAt = Instant.now();
        long startedAt = System.nanoTime();
        int status = 500;
        try {
            chain.doFilter(request, response);
            status = response.getStatus();
        } finally {
            long durationMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAt);
            String result = describe(status);
            Instant finishedAt = Instant.now();
            Long paymentProcessMs = (Long) request.getAttribute(PAYMENT_PROCESS_MS);
            record(receivedAt, finishedAt, status, durationMs, paymentProcessMs, result);
            LOG.info("SePay webhook received_at={} finished_at={} status={} duration_ms={} payment_process_ms={} result={}",
                receivedAt, finishedAt, status, durationMs, paymentProcessMs, result);
        }
    }

    private synchronized void record(Instant receivedAt, Instant finishedAt, int status, long durationMs, Long paymentProcessMs, String result) {
        if (deliveries.size() >= MAX_DELIVERIES) deliveries.removeLast();
        deliveries.addFirst(new Delivery(++sequence, receivedAt, finishedAt, status, durationMs, paymentProcessMs, result));
    }

    private String describe(int status) {
        return switch (status) {
            case 200, 201 -> "Delivery accepted";
            case 400 -> "Invalid or missing transaction data";
            case 401, 403 -> "Authentication rejected";
            case 404 -> "Webhook route unavailable";
            case 409 -> "Processing conflict";
            case 413 -> "Request too large";
            case 415 -> "Unsupported request format";
            case 503 -> "SePay receiver unavailable";
            default -> status >= 500 ? "Server processing error" : "HTTP " + status;
        };
    }
}
