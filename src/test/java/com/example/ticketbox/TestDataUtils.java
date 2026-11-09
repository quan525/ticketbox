package com.example.ticketbox;

import com.example.ticketbox.enums.SeatType;
import com.example.ticketbox.movie.dto.request.CreateMovieRequest;
import com.example.ticketbox.room.dto.request.CreateRoomRequest;
import com.example.ticketbox.seat.dto.request.CreateSeatRequest;
import com.example.ticketbox.seat.dto.request.UpdateSeatRequest;
import com.example.ticketbox.show.dto.Request.CreateShowReq;
import com.example.ticketbox.user.dto.request.CreateUserRequest;

import java.time.LocalDateTime;

public final class TestDataUtils {
    private TestDataUtils() {
    }

    public static CreateUserRequest createUserRequestA() {

        new CreateUserRequest();
        return CreateUserRequest.builder()
                .username("beanchloe")
                .password("12345678")
                .build();

    }


    public static CreateUserRequest createUserRequestB() {
        return CreateUserRequest.builder()
                .username("Dominic")
                .password("Carter")
                .build();
    }

    public static CreateUserRequest createUserRequestC() {
        return CreateUserRequest.builder()
                .username("Dominic")
                .password("Carter")
                .build();
    }

    public static CreateMovieRequest CreateMovieRequestA() {
        return CreateMovieRequest.builder()
                .title("Onyx light")
                .author("Henderson")
                .durationMin(124)
                .language("EN")
                .build();
    }

    public static CreateRoomRequest CreateRoomRequestA() {
        return CreateRoomRequest.builder()
                .name("Room A")
                .seatRows(21)
                .build();
    }

    public static CreateRoomRequest CreateRoomRequestB() {
        return CreateRoomRequest.builder()
                .name("Room B")
                .seatRows(21)
                .build();
    }

    public static CreateRoomRequest CreateRoomRequestE() {
        return CreateRoomRequest.builder()
                .name("Room E")
                .seatRows(21)
                .build();
    }

    public static CreateSeatRequest CreateSeatRequest(String rowLabel, Integer seatNumber) {
        return CreateSeatRequest.builder()
                .rowLabel(rowLabel)
                .seatNumber(seatNumber)
                .seatType(SeatType.STANDARD)
                .build();
    }

    public static CreateSeatRequest CreateSeatRequestA2() {
        return CreateSeatRequest.builder()
                .rowLabel("A")
                .seatNumber(2)
                .seatType(SeatType.STANDARD)
                .build();
    }

    public static CreateSeatRequest CreateSeatRequestA3() {
        return CreateSeatRequest.builder()
                .rowLabel("A")
                .seatNumber(3)
                .seatType(SeatType.STANDARD)
                .build();
    }

    public static CreateShowReq CreateShowRequestA1(Integer movieId, Integer roomId) {
        return CreateShowReq.builder()
                .startTime(LocalDateTime.of(2026, 1, 16, 9, 0))
                .endTime(LocalDateTime.of(2026, 1, 16, 11, 0))
                .build();
    }

    public static UpdateSeatRequest CreateUpdateSeatReq() {
        return UpdateSeatRequest.builder().seatType(SeatType.VIP).build();
    }


}