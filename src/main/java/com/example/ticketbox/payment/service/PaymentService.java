package com.example.ticketbox.payment.service;

import com.example.ticketbox.entity.Payments;
import com.example.ticketbox.payment.dto.request.CreatePaymentRequest;

import java.util.List;

public interface PaymentService {
    Payments createPayment(CreatePaymentRequest request, String username);

    Payments getPayment(Long id, String username);

    List<Payments> getPaymentsByBooking(Long bookingId, String username);

    Payments completePayment(Long id, String transactionId, String username);

    Payments cancelPayment(Long id, String username);
}
