package com.example.ticketbox.showseat.dto.request;

import com.example.ticketbox.enums.SeatStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Getter
@RequiredArgsConstructor
@Builder
@AllArgsConstructor
public class CreateShowSeatReq {
    @NotNull
    @Positive
    private Integer showId;

    @NotNull
    @Positive
    private List<Integer> seatIds;

    private SeatStatus seatStatus;

    @NotNull
    @Positive
    private BigDecimal price;
}