package com.example.ticketbox.service;

import com.example.ticketbox.TestDataFactory;
import com.example.ticketbox.booking.dto.request.CreateBookingReq;
import com.example.ticketbox.booking.service.BookingServiceImpl;
import com.example.ticketbox.entity.*;
import com.example.ticketbox.enums.BookingStatus;
import com.example.ticketbox.show.dto.Request.CreateShowReq;
import com.example.ticketbox.showseat.service.ShowSeatServiceImpl;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@org.springframework.transaction.annotation.Transactional
class BookingServiceTest {

    @Autowired
    BookingServiceImpl bookingService;

    @Autowired
    TestDataFactory dataFactory;

    @Autowired
    ShowSeatServiceImpl showSeatService;

    /* ---------------- helpers ---------------- */

    private CreateBookingReq buildCreateBookingReq() {
        Users user = dataFactory.createUser();

        Rooms room = dataFactory.createRoom();
        dataFactory.createSeatsForRoom(room);

        Movies movie = dataFactory.createMovie();

        Show show = dataFactory.createShow(
                CreateShowReq.builder()
                        .roomId(room.getId())
                        .movieId(movie.getId())
                        .build()
        );

        showSeatService.generateForShow(show.getId());

        List<ShowSeat> showSeats =
                showSeatService.getShowSeatsByShowId(show.getId());

        return CreateBookingReq.builder()
                .userId(user.getId())
                .showId(show.getId())
//                .status(BookingStatus.PENDING)
                .totalPrice(new BigDecimal(233))
                .showSeatIds(
                        showSeats.stream()
                                .limit(2)
                                .map(ShowSeat::getId)
                                .toList()
                )
                .build();
    }

    /* ---------------- createBooking ---------------- */

    @Test
    void createBooking_success() {
        CreateBookingReq req = buildCreateBookingReq();

        Booking booking = bookingService.createBooking(req);


        List<Booking> bookings = bookingService.getBookings();
        assertNotNull(booking.getId());
        assertEquals(BookingStatus.PENDING, booking.getStatus());
        assertEquals(0, new BigDecimal("200000").compareTo(booking.getTotalPrice()));
        assertTrue(booking.getBookingSeats().stream().allMatch(seat -> seat.getShowSeat().getStatus() == com.example.ticketbox.enums.SeatStatus.PENDING));

//        BigDecimal expectedTotal = booking.getBookingSeats().stream()
//                .map(BookingSeat::getPrice)
//                .reduce(BigDecimal.ZERO, BigDecimal::add);
//        BigDecimal price = booking.getTotalPrice();
//        assertEquals(expectedTotal, booking.getTotalPrice());
    }

    @Test
    void createBooking_userNotFound() {
        CreateBookingReq req = buildCreateBookingReq();
        req.setUserId(99999);

        EntityNotFoundException ex =
                org.junit.jupiter.api.Assertions.assertThrows(
                        EntityNotFoundException.class,
                        () -> bookingService.createBooking(req)
                );

        assertEquals("User not found", ex.getMessage());
    }

    @Test
    void createBooking_showNotFound() {
        CreateBookingReq req = buildCreateBookingReq();
        req.setShowId(1233);

        EntityNotFoundException ex =
                org.junit.jupiter.api.Assertions.assertThrows(
                        EntityNotFoundException.class,
                        () -> bookingService.createBooking(req)
                );

        assertEquals("Show not found", ex.getMessage());
    }

    @Test
    void createBooking_someShowSeatsMissing() {
        CreateBookingReq req = buildCreateBookingReq();
        req.setShowSeatIds(List.of(Integer.MAX_VALUE));

        EntityNotFoundException ex = assertThrows(
                EntityNotFoundException.class,
                () -> bookingService.createBooking(req)
        );

        assertEquals("One or more show seats not found", ex.getMessage());
    }


    @Test
    void getBooking_success() {
        Booking created = bookingService.createBooking(buildCreateBookingReq());

        Booking found = bookingService.getBooking(created.getId());

        assertEquals(created.getId(), found.getId());
    }

    @Test
    void getBooking_notFound() {
        EntityNotFoundException ex =
                org.junit.jupiter.api.Assertions.assertThrows(
                        EntityNotFoundException.class,
                        () -> bookingService.getBooking(99999L)
                );

        assertEquals("Booking not found", ex.getMessage());
    }


    @Test
    void cancelBooking_pending_success() {
        Booking booking = bookingService.createBooking(buildCreateBookingReq());

        Booking cancelled = bookingService.cancelBooking(booking.getId());

        assertEquals(BookingStatus.CANCELLED, cancelled.getStatus());
        assertTrue(cancelled.getBookingSeats().stream().allMatch(seat -> seat.getShowSeat().getStatus() == com.example.ticketbox.enums.SeatStatus.AVAILABLE));
    }

    @Test void duplicateSeatsAreRejected() {
        var req = buildCreateBookingReq();
        req.setShowSeatIds(List.of(req.getShowSeatIds().get(0), req.getShowSeatIds().get(0)));
        assertThrows(IllegalArgumentException.class, () -> bookingService.createBooking(req));
    }

    @Test void alreadyHeldSeatsCannotBeBookedAgain() {
        var req = buildCreateBookingReq();
        bookingService.createBooking(req);
        assertThrows(IllegalStateException.class, () -> bookingService.createBooking(req));
    }

    @Test void expiredHoldsBecomeAvailableWhenLoadingSeatMap() {
        var booking = bookingService.createBooking(buildCreateBookingReq());
        booking.getBookingSeats().forEach(seat -> seat.getShowSeat().setHeldUntil(java.time.LocalDateTime.now().minusSeconds(1)));
        var seats = showSeatService.getShowSeatsByShowId(booking.getShow().getId());
        assertTrue(seats.stream().allMatch(seat -> seat.getStatus() == com.example.ticketbox.enums.SeatStatus.AVAILABLE));
    }

    @Test void cancellingAnOldBookingDoesNotReleaseANewerHold() {
        var req = buildCreateBookingReq();
        var oldBooking = bookingService.createBooking(req);
        oldBooking.setExpiresAt(java.time.LocalDateTime.now().minusMinutes(1));
        oldBooking.getBookingSeats().forEach(seat -> seat.getShowSeat().setHeldUntil(oldBooking.getExpiresAt()));
        showSeatService.getShowSeatsByShowId(oldBooking.getShow().getId());
        var newBooking = bookingService.createBooking(req);
        bookingService.cancelBooking(oldBooking.getId());
        assertTrue(newBooking.getBookingSeats().stream().allMatch(seat -> seat.getShowSeat().getStatus() == com.example.ticketbox.enums.SeatStatus.PENDING));
    }

}

