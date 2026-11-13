package com.example.ticketbox.showseat.dto.response;

import com.example.ticketbox.entity.Seats;
import com.example.ticketbox.entity.Show;
import com.example.ticketbox.enums.SeatStatus;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShowSeatRes {
    private Integer id;

    private com.example.ticketbox.show.dto.Response.ShowResponse show;

    private com.example.ticketbox.seat.dto.response.SeatResponse seat;

    private SeatStatus status;

    private BigDecimal price;

    private Instant heldUntil;
}
