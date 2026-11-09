package com.example.ticketbox.seat.dto.request;

import com.example.ticketbox.enums.SeatType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Builder
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class UpdateSeatRequest {

    private SeatType seatType;
}


