package com.example.ticketbox.payment.controller;

import com.example.ticketbox.payment.dto.PaymentMapper;
import com.example.ticketbox.payment.dto.response.PaymentResponse;
import com.example.ticketbox.payment.service.MockPaymentService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/payments")
@RequiredArgsConstructor
public class MockPaymentController {
    private final MockPaymentService mock;
    private final PaymentMapper mapper;
    public record Configuration(boolean enabled, String provider) {}
    public record Confirmation(@NotNull MockPaymentService.Outcome outcome) {}

    @GetMapping("/mock/config")
    public ResponseEntity<Configuration> configuration() {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(new Configuration(mock.enabled(), "SEPAY_QR_PREVIEW"));
    }
    @GetMapping("/{id}/mock-checkout")
    public ResponseEntity<MockPaymentService.Checkout> checkout(@PathVariable Long id, Authentication authentication) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(mock.checkout(id, authentication.getName()));
    }
    @PostMapping("/{id}/mock-confirm")
    public ResponseEntity<PaymentResponse> confirm(@PathVariable Long id, @Valid @RequestBody Confirmation request, Authentication authentication) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(mapper.toResponse(mock.confirm(id, authentication.getName(), request.outcome())));
    }
}
