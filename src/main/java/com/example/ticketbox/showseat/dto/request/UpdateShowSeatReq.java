package com.example.ticketbox.showseat.dto.request;

import com.example.ticketbox.enums.SeatStatus;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;

@Getter
@Builder
@RequiredArgsConstructor
@AllArgsConstructor
public class UpdateShowSeatReq {
    @Builder.Default
    private SeatStatus seatStatus = SeatStatus.AVAILABLE;

    @Positive
    private BigDecimal price;
}