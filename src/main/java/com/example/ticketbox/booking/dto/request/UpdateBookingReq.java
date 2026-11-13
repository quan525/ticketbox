package com.example.ticketbox.booking.dto.request;

import com.example.ticketbox.entity.Users;
import com.example.ticketbox.enums.BookingStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

@Builder
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class UpdateBookingReq {
    private Users user;

    private BookingStatus status;

    private BigDecimal totalPrice;

    private Instant expiresAt;
}