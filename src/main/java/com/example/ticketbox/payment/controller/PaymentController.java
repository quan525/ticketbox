package com.example.ticketbox.payment.controller;

import com.example.ticketbox.payment.dto.PaymentMapper;
import com.example.ticketbox.payment.dto.request.CompletePaymentRequest;
import com.example.ticketbox.payment.dto.request.CreatePaymentRequest;
import com.example.ticketbox.payment.dto.response.PaymentResponse;
import com.example.ticketbox.payment.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/payments")
@RequiredArgsConstructor
public class PaymentController {
    private final PaymentService paymentService;
    private final PaymentMapper paymentMapper;

    @PostMapping
    public ResponseEntity<PaymentResponse> createPayment(
            @RequestBody @Valid CreatePaymentRequest request,
            Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
                paymentMapper.toResponse(
                        paymentService.createPayment(request, authentication.getName())));
    }

    @GetMapping("/{id}")
    public PaymentResponse getPayment(@PathVariable Long id, Authentication authentication) {
        return paymentMapper.toResponse(paymentService.getPayment(id, authentication.getName()));
    }

    @GetMapping("/booking/{bookingId}")
    public List<PaymentResponse> getPaymentsByBooking(
            @PathVariable Long bookingId,
            Authentication authentication) {
        return paymentService.getPaymentsByBooking(bookingId, authentication.getName())
                .stream()
                .map(paymentMapper::toResponse)
                .toList();
    }

    @PatchMapping("/{id}/complete")
    public PaymentResponse completePayment(
            @PathVariable Long id,
            @RequestBody @Valid CompletePaymentRequest request,
            Authentication authentication) {
        return paymentMapper.toResponse(paymentService.completePayment(
                id, request.getTransactionId(), authentication.getName()));
    }

    @PatchMapping("/{id}/cancel")
    public PaymentResponse cancelPayment(@PathVariable Long id, Authentication authentication) {
        return paymentMapper.toResponse(paymentService.cancelPayment(id, authentication.getName()));
    }
}
