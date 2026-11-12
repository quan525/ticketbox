package com.example.ticketbox.service;

import com.example.ticketbox.TestDataFactory;
import com.example.ticketbox.booking.dto.request.CreateBookingReq;
import com.example.ticketbox.booking.service.BookingService;
import com.example.ticketbox.entity.*;
import com.example.ticketbox.enums.*;
import com.example.ticketbox.payment.dto.request.CreatePaymentRequest;
import com.example.ticketbox.payment.service.*;
import com.example.ticketbox.show.dto.Request.CreateShowReq;
import com.example.ticketbox.showseat.service.ShowSeatService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import java.util.List;
import java.util.concurrent.*;
import java.util.function.Supplier;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest(properties = {"payments.mock.enabled=true", "spring.application.name=reservation-concurrency-test"})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class ReservationConcurrencyTest {
    @Autowired TestDataFactory data;
    @Autowired BookingService bookings;
    @Autowired ShowSeatService seats;
    @Autowired PaymentService payments;
    @Autowired MockPaymentService mock;
    @Autowired PlatformTransactionManager transactions;

    private List<Object> race(Supplier<?> first, Supplier<?> second) throws Exception {
        var ready = new CountDownLatch(2); var start = new CountDownLatch(1);
        var pool = Executors.newFixedThreadPool(2);
        try {
            var futures = List.of(first, second).stream().map(action -> pool.submit(() -> {
                ready.countDown();
                if (!start.await(10, TimeUnit.SECONDS)) throw new IllegalStateException("Race did not start");
                try { return (Object) action.get(); } catch (RuntimeException failure) { return failure; }
            })).toList();
            assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue(); start.countDown();
            return List.of(futures.get(0).get(20, TimeUnit.SECONDS), futures.get(1).get(20, TimeUnit.SECONDS));
        } finally { start.countDown(); pool.shutdownNow(); }
    }

    @Test void simultaneousReservationsAndPaymentTransitionsRemainConsistent() throws Exception {
        var tx = new TransactionTemplate(transactions);
        var req = tx.execute(status -> {
            var user = data.createUser(); var room = data.createRoom(); data.createSeatsForRoom(room);
            var movie = data.createMovie(); var show = data.createShow(CreateShowReq.builder().roomId(room.getId()).movieId(movie.getId()).build());
            seats.generateForShow(show.getId());
            return CreateBookingReq.builder().userId(user.getId()).showId(show.getId())
                    .showSeatIds(seats.getShowSeatsByShowId(show.getId()).stream().limit(2).map(ShowSeat::getId).toList()).build();
        });
        var reservations = race(() -> bookings.createBooking(req), () -> bookings.createBooking(req));
        assertThat(reservations.stream().filter(Booking.class::isInstance).count()).isEqualTo(1);
        assertThat(reservations.stream().filter(IllegalStateException.class::isInstance).count()).isEqualTo(1);
        var booking = (Booking) reservations.stream().filter(Booking.class::isInstance).findFirst().orElseThrow();
        String username = tx.execute(status -> bookings.getBooking(booking.getId()).getUser().getUsername());
        var paymentReq = CreatePaymentRequest.builder().bookingId(booking.getId()).method(PaymentMethods.BANK_TRANSFER).build();
        var paymentResults = race(() -> payments.createPayment(paymentReq, username), () -> payments.createPayment(paymentReq, username));
        assertThat(paymentResults.stream().filter(Payments.class::isInstance).count()).isEqualTo(1);
        assertThat(paymentResults.stream().filter(IllegalStateException.class::isInstance).count()).isEqualTo(1);
        var payment = (Payments) paymentResults.stream().filter(Payments.class::isInstance).findFirst().orElseThrow();
        var transitions = race(() -> mock.confirm(payment.getId(), username, MockPaymentService.Outcome.SUCCESS), () -> bookings.cancelBooking(booking.getId()));
        assertThat(transitions.stream().filter(RuntimeException.class::isInstance).count()).isEqualTo(1);
        tx.executeWithoutResult(status -> {
            var finalBooking = bookings.getBooking(booking.getId());
            var finalPayment = payments.getPayment(payment.getId(), username);
            if (finalBooking.getStatus() == BookingStatus.CONFIRMED) {
                assertThat(finalPayment.getStatus()).isEqualTo(PaymentStatus.PAID);
                assertThat(finalBooking.getBookingSeats()).allSatisfy(seat -> assertThat(seat.getShowSeat().getStatus()).isEqualTo(SeatStatus.BOOKED));
            } else {
                assertThat(finalBooking.getStatus()).isEqualTo(BookingStatus.CANCELLED);
                assertThat(finalPayment.getStatus()).isEqualTo(PaymentStatus.CANCELLED);
                assertThat(finalBooking.getBookingSeats()).allSatisfy(seat -> assertThat(seat.getShowSeat().getStatus()).isEqualTo(SeatStatus.AVAILABLE));
            }
        });
        var rollbackPayment = tx.execute(status -> {
            var seatId = seats.getShowSeatsByShowId(req.getShowId()).stream()
                    .filter(seat -> seat.getStatus() == SeatStatus.AVAILABLE).findFirst().orElseThrow().getId();
            var held = bookings.createBooking(CreateBookingReq.builder().userId(req.getUserId()).showId(req.getShowId()).showSeatIds(List.of(seatId)).build());
            return payments.createPayment(CreatePaymentRequest.builder().bookingId(held.getId()).method(PaymentMethods.BANK_TRANSFER).build(), username).getId();
        });
        assertThatThrownBy(() -> tx.executeWithoutResult(status -> {
            mock.confirm(rollbackPayment, username, MockPaymentService.Outcome.SUCCESS);
            throw new IllegalStateException("Force rollback after all payment updates");
        })).isInstanceOf(IllegalStateException.class);
        tx.executeWithoutResult(status -> {
            var rolledBack = payments.getPayment(rollbackPayment, username);
            assertThat(rolledBack.getStatus()).isEqualTo(PaymentStatus.PENDING);
            assertThat(rolledBack.getTransactionId()).isNull();
            assertThat(rolledBack.getBooking().getStatus()).isEqualTo(BookingStatus.PENDING);
            assertThat(rolledBack.getBooking().getBookingSeats()).allSatisfy(seat -> {
                assertThat(seat.getShowSeat().getStatus()).isEqualTo(SeatStatus.PENDING);
                assertThat(seat.getShowSeat().getHeldUntil()).isNotNull();
            });
        });
    }
}
