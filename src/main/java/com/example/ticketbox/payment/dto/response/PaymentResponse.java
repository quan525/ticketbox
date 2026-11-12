package com.example.ticketbox.payment.dto.response;

import com.example.ticketbox.enums.PaymentMethods;
import com.example.ticketbox.enums.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentResponse {
    private Long id;
    private String checkoutProvider;
    private String sepayReference;
    private Long bookingId;
    private PaymentStatus status;
    private PaymentMethods method;
    private String transactionId;
    private BigDecimal amount;
    private LocalDateTime paidAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
