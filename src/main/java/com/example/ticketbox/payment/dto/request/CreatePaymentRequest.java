package com.example.ticketbox.payment.dto.request;

import com.example.ticketbox.enums.PaymentMethods;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreatePaymentRequest {

    @NotNull
    @Positive
    private Long bookingId;

    @NotNull
    private PaymentMethods method;
}
