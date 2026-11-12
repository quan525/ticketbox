package com.example.ticketbox.payment.controller;

import com.example.ticketbox.entity.SepayEvent;
import com.example.ticketbox.payment.sepay.*;
import com.example.ticketbox.payment.service.*;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import java.util.concurrent.TimeUnit;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.ObjectMapper;
import java.util.*;

@RestController @RequestMapping("/payments") @RequiredArgsConstructor
public class SepayPaymentController {
    private final SepaySettings settings;
    private final SepayAuthentication authentication;
    private final SepayPaymentService sepay;
    private final MockPaymentService mock;
    private final SepayEventRepository events;
    private final SepayDeliveryLog deliveryLog;
    // External SePay payloads must not inherit the application API snake_case naming policy.
    private final ObjectMapper webhookMapper = tools.jackson.databind.json.JsonMapper.builder().build();

    public record Configuration(String provider, boolean enabled, boolean demo) {}
    // Return actionable, non-sensitive webhook errors instead of Spring's generic error page.
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, Object>> statusError(ResponseStatusException exception) {
        return ResponseEntity.status(exception.getStatusCode()).cacheControl(CacheControl.noStore()).body(Map.of(
            "success", false, "status", exception.getStatusCode().value(),
            "message", exception.getReason() == null ? "Invalid SePay request" : exception.getReason()));
    }
    @ExceptionHandler(org.springframework.http.converter.HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> unreadableBody() {
        return ResponseEntity.badRequest().body(Map.of("success", false, "status", 400,
            "message", "Send a non-empty raw JSON object with Content-Type application/json"));
    }
    @GetMapping("/config")
    public ResponseEntity<Configuration> config() {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(new Configuration(
            settings.isEnabled() ? settings.provider() : "LOCAL_MOCK", settings.isEnabled() || mock.enabled(), !settings.isEnabled() || settings.demo()));
    }
    @GetMapping("/{id}/sepay-checkout")
    public ResponseEntity<SepayPaymentService.Checkout> checkout(@PathVariable Long id, Authentication actor) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(sepay.checkout(id, actor.getName()));
    }
    @GetMapping("/sepay/deliveries")
    public ResponseEntity<List<SepayDeliveryLog.Delivery>> deliveries() {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(deliveryLog.recent());
    }

    @GetMapping("/sepay/events")
    public ResponseEntity<List<SepayEvent>> events() {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(events.findTop100ByOrderByCreatedAtDesc());
    }
    @PostMapping(value = "/sepay/webhook", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Boolean>> webhook(@RequestHeader HttpHeaders headers, @RequestBody byte[] body, HttpServletRequest request) {
        if (body.length > 65536) throw new ResponseStatusException(HttpStatus.valueOf(413));
        authentication.verify(headers, body);
        SepayPayload payload;
        try { payload = webhookMapper.readValue(body, SepayPayload.class); }
        catch (RuntimeException exception) { throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid webhook JSON or field types. Send a JSON object with numeric id and transferAmount, and camelCase SePay field names."); }
        if (payload == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Webhook body must be a JSON object, not null");
        long paymentStartedAt = System.nanoTime();
        try { sepay.receive(payload); }
        catch (org.springframework.dao.DataIntegrityViolationException exception) {
            // A racing delivery may lose the unique-insert race. The service transaction has rolled back.
            if (!events.existsByModeAndTransactionId(settings.getMode().name(), payload.id())) throw exception;
        }
        finally {
            request.setAttribute(SepayDeliveryLog.PAYMENT_PROCESS_MS,
                TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - paymentStartedAt));
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("success", true));
    }
}
