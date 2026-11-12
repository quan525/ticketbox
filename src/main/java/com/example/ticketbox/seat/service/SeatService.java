package com.example.ticketbox.seat.service;

import com.example.ticketbox.entity.Seats;
import com.example.ticketbox.seat.dto.request.AutoGenerateSeatReq;
import com.example.ticketbox.seat.dto.request.CreateSeatRequest;
import com.example.ticketbox.seat.dto.request.UpdateSeatRequest;

import java.util.List;

public interface SeatService {

    Seats create(Integer roomId, CreateSeatRequest req);


    List<Seats> getSeatsByRoom(Integer roomId);

    Seats updateSeat(Integer seatId, UpdateSeatRequest req);

    List<Seats> autoGenerateSeats(
            Integer roomId,
            AutoGenerateSeatReq req
    );

    List<Seats> getSeatsByRoomForScheduling(Integer roomId);

    List<Seats> getSeatInIds(List<Integer> ids);

//    Seat updateSeatType(Integer seatId, UpdateSeatRequest req);
}
