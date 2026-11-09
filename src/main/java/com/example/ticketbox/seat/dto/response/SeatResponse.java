package com.example.ticketbox.seat.dto.response;

import com.example.ticketbox.entity.Rooms;
import com.example.ticketbox.enums.SeatType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SeatResponse {
    Integer id;

    Integer roomid;

    String rowLabel;
    Integer seatNumber;
    SeatType seatType;
}