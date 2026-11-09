package com.example.ticketbox.service;

import com.example.ticketbox.TestDataUtils;
import com.example.ticketbox.entity.Rooms;
import com.example.ticketbox.entity.Seats;
import com.example.ticketbox.enums.SeatType;
import com.example.ticketbox.room.dto.request.CreateRoomRequest;
import com.example.ticketbox.room.service.RoomService;
import com.example.ticketbox.seat.dto.request.AutoGenerateSeatReq;
import com.example.ticketbox.seat.dto.request.CreateSeatRequest;
import com.example.ticketbox.seat.service.SeatService;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

import java.util.List;

import static com.example.ticketbox.TestDataUtils.CreateUpdateSeatReq;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@org.springframework.transaction.annotation.Transactional
class SeatsServiceTest {

    @Autowired
    private RoomService roomService;
    @Autowired
    private SeatService seatService;

    @Test
    void createSeat_success() {
        CreateRoomRequest roomReq = CreateRoomRequest.builder().name("24").seatRows(6).build();

        Rooms roomRes = roomService.create(roomReq);

        CreateSeatRequest seatReq = TestDataUtils.CreateSeatRequest("A", 1);

        Seats seatRes = seatService.create(roomRes.getId(), seatReq);

        assertThat(seatRes.getSeatType()).isEqualTo(SeatType.STANDARD);
    }

    @Test
    void createSeat_roomNotFound() {
//        CreateRoomRequest roomReq = TestDataUtils.CreateRoomRequestA();

        Integer roomId = 1;
//        Rooms response = roomService.create(roomReq);

        CreateSeatRequest req = TestDataUtils.CreateSeatRequest("A", 1);

        assertThatThrownBy(() -> seatService.create(roomId, req))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Room not found");

    }

    @Test
    void updateSeat() {
        CreateRoomRequest roomReq = CreateRoomRequest.builder().name("mel").seatRows(21).build();

        Rooms roomRes = roomService.create(roomReq);


        CreateSeatRequest seatReq = TestDataUtils.CreateSeatRequest("A", 1);

        Seats seatRes = seatService.create(roomRes.getId(), seatReq);

        Seats updateRes = seatService.updateSeat(seatRes.getId(), CreateUpdateSeatReq());

    }
    @Test
    void autoGenerateSeats_success() {
        // Arrange
        CreateRoomRequest roomXReq = CreateRoomRequest.builder().name("room x").seatRows(12).build();
        Rooms room = roomService.create(roomXReq);

        AutoGenerateSeatReq req = AutoGenerateSeatReq.builder()
                .rows(2)                 // A, B
                .seatsPerRow(3)          // 1,2,3
                .startSeatNumber(1)
                .seatType(SeatType.STANDARD)
                .build();

        // Act
        List<Seats> result = seatService.autoGenerateSeats(room.getId(), req);

        // Assert
        assertNotNull(result);
        assertEquals(6, result.size());

        // Validate a few samples
        assertTrue(result.stream()
                .anyMatch(s -> s.getRowLabel().equals("A") && s.getSeatNumber() == 1));

        assertTrue(result.stream()
                .anyMatch(s -> s.getRowLabel().equals("B") && s.getSeatNumber() == 3));

        assertTrue(result.stream()
                .allMatch(s -> s.getSeatType() == SeatType.STANDARD));
    }

}
