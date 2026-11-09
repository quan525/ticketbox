package com.example.ticketbox.service;

import com.example.ticketbox.TestDataFactory;
import com.example.ticketbox.entity.*;
import com.example.ticketbox.enums.SeatType;
import com.example.ticketbox.room.dto.request.CreateRoomRequest;
import com.example.ticketbox.room.service.RoomServiceImpl;
import com.example.ticketbox.seat.dto.request.CreateSeatRequest;
import com.example.ticketbox.seat.service.SeatServiceImpl;
import com.example.ticketbox.show.dto.Request.CreateShowReq;
import com.example.ticketbox.show.service.ShowServiceImpl;
import com.example.ticketbox.showseat.dto.request.CreateShowSeatReq;
import com.example.ticketbox.showseat.service.ShowSeatServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
@org.springframework.transaction.annotation.Transactional
public class ShowSeatServiceTest {

    @Autowired
    TestDataFactory dataFactory;
    @Autowired
    SeatServiceImpl seatService;

    @Autowired
    ShowServiceImpl showService;

    @Autowired
    RoomServiceImpl roomService;

    @Autowired
    ShowSeatServiceImpl showSeatService;

    @Test
    void createSeatShowTest_shouldSkipBecauseShowSeatsAlreadyExist() {

        // Arrange
        Rooms room = roomService.create(CreateRoomRequest.builder().name("Kinder").seatRows(22).build());
        Movies movie = dataFactory.createMovie();

        Seats seat = seatService.create(
                room.getId(),
                CreateSeatRequest.builder()
                        .seatNumber(12)
                        .rowLabel("B")
                        .seatType(SeatType.VIP)
                        .build()
        );

        Show show = showService.createShow(
                CreateShowReq.builder()
                        .roomId(room.getId())
                        .movieId(movie.getId())
                        .build()
        );

        CreateShowSeatReq ssReq = CreateShowSeatReq.builder()
                .showId(show.getId())
                .seatIds(List.of(seat.getId()))
                .price(new BigDecimal("12321312.000"))
                .build();

        // Act
        List<ShowSeat> result = showSeatService.createShowSeats(ssReq);

        // Assert
        assertNotNull(result);
        org.junit.jupiter.api.Assertions.assertEquals(1, result.size());
        org.junit.jupiter.api.Assertions.assertEquals(com.example.ticketbox.enums.SeatStatus.AVAILABLE, result.get(0).getStatus());
        org.junit.jupiter.api.Assertions.assertTrue(showSeatService.createShowSeats(ssReq).isEmpty());
//        assertTrue(result.isEmpty());
    }
}
