package com.example.ticketbox;

import com.example.ticketbox.entity.*;
import com.example.ticketbox.enums.SeatType;
import com.example.ticketbox.movie.service.MovieServiceImpl;
import com.example.ticketbox.room.service.RoomServiceImpl;
import com.example.ticketbox.seat.dto.request.AutoGenerateSeatReq;
import com.example.ticketbox.seat.service.SeatServiceImpl;
import com.example.ticketbox.show.dto.Request.CreateShowReq;
import com.example.ticketbox.show.service.ShowServiceImpl;
import com.example.ticketbox.user.service.UserServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import static com.example.ticketbox.TestDataUtils.*;

@Component
@RequiredArgsConstructor
public class TestDataFactory {
    private final RoomServiceImpl roomService;
    private final SeatServiceImpl seatService;
    private final MovieServiceImpl movieService;
    private final UserServiceImpl userService;
    private final ShowServiceImpl showService;

    public Users createUser() {
        return userService.createUser(createUserRequestC());
    }

    public Rooms createRoom() {
        return roomService.create(CreateRoomRequestE());
    }

    public Seats createSeat(String rowLabel, Integer seatNumber) {
        Rooms room = createRoom();
        return seatService.create(room.getId(), TestDataUtils.CreateSeatRequest(rowLabel, seatNumber));
    }
    public void createSeatsForRoom(Rooms room) {
        seatService.autoGenerateSeats(
                room.getId(),
                AutoGenerateSeatReq.builder()
                        .rows(3)
                        .seatsPerRow(5)
                        .startSeatNumber(1)
                        .seatType(SeatType.STANDARD)
                        .build()
        );
    }

    public Movies createMovie() {
        return movieService.create(CreateMovieRequestA());
    }

    public Show createShow(CreateShowReq createShowReq) {
        return showService.createShow(createShowReq);
    }
}
