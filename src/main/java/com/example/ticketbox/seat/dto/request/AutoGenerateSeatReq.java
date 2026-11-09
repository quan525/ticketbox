package com.example.ticketbox.seat.dto.request;

import com.example.ticketbox.enums.SeatType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AutoGenerateSeatReq {

    @Positive
    private int rows;          // number of rows (A, B, C...)

    @Positive
    private int seatsPerRow;   // seats per row (1..N)

    @NotNull
    private SeatType seatType; // DEFAULT / VIP / COUPLE

    // optional
    @Builder.Default
    private int startSeatNumber = 1;
}
