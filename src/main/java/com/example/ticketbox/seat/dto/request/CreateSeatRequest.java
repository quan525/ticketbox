package com.example.ticketbox.seat.dto.request;

import com.example.ticketbox.enums.SeatType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Builder
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class CreateSeatRequest {
    @NotBlank
    private String rowLabel; // A, B, C

    @NotNull
    @Positive
    private int seatNumber;

    private SeatType seatType;
}
