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
import org.springframework.security.access.AccessDeniedException;
import org.springframework.transaction.annotation.Transactional;
import jakarta.persistence.EntityManager;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest(properties = "payments.mock.enabled=true")
@Transactional
class MockPaymentServiceTest {
    @Autowired TestDataFactory data;
    @Autowired BookingService bookings;
    @Autowired ShowSeatService seats;
    @Autowired PaymentService payments;
    @Autowired MockPaymentService mock;
    @Autowired EntityManager em;

    private Payments payment() {
        var user = data.createUser(); var room = data.createRoom(); data.createSeatsForRoom(room);
        var movie = data.createMovie();
        var show = data.createShow(CreateShowReq.builder().roomId(room.getId()).movieId(movie.getId()).build());
        seats.generateForShow(show.getId());
        var booking = bookings.createBooking(CreateBookingReq.builder().userId(user.getId()).showId(show.getId())
                .showSeatIds(seats.getShowSeatsByShowId(show.getId()).stream().limit(2).map(ShowSeat::getId).toList()).build());
        return payments.createPayment(CreatePaymentRequest.builder().bookingId(booking.getId()).method(PaymentMethods.BANK_TRANSFER).build(), user.getUsername());
    }
    @Test void checkoutUsesServerAmountAndDemoOnlyPayload() {
        var payment = payment();
        var checkout = mock.checkout(payment.getId(), payment.getBooking().getUser().getUsername());
        assertThat(checkout.amount()).isEqualByComparingTo(payment.getAmount());
        assertThat(checkout.currency()).isEqualTo("VND");
        assertThat(checkout.qrPayload()).startsWith("000201010212").contains("A000000727", "QRIBFTTA", "5303704");
        assertThat(checkout.bankBin()).isEqualTo("000000");
        assertThat(checkout.accountNo()).isEqualTo("000000000000");
        assertThat(checkout.reference()).isEqualTo("TBTEST" + payment.getId());
        assertThat(checkout.demo()).isTrue();
        assertThat(checkout.qrImageUrl()).isEqualTo("https://img.vietqr.io/image/ICB-113366668888-compact.svg");
    }
    @Test void successIsAtomicAndRetryIsIdempotent() {
        var payment = payment(); var username = payment.getBooking().getUser().getUsername();
        mock.confirm(payment.getId(), username, MockPaymentService.Outcome.SUCCESS);
        em.flush(); em.clear();
        var paid = mock.confirm(payment.getId(), username, MockPaymentService.Outcome.SUCCESS);
        assertThat(paid.getStatus()).isEqualTo(PaymentStatus.PAID);
        assertThat(paid.getTransactionId()).startsWith("MOCK-TICKETBOX-");
        assertThat(paid.getBooking().getStatus()).isEqualTo(BookingStatus.CONFIRMED);
        assertThat(paid.getBooking().getBookingSeats()).allSatisfy(seat -> {
            assertThat(seat.getShowSeat().getStatus()).isEqualTo(SeatStatus.BOOKED);
            assertThat(seat.getShowSeat().getHeldUntil()).isNull();
        });
    }
    @Test void declineKeepsReservationAndAllowsAnotherPayment() {
        var payment = payment(); var username = payment.getBooking().getUser().getUsername();
        var result = mock.confirm(payment.getId(), username, MockPaymentService.Outcome.DECLINED);
        assertThat(result.getStatus()).isEqualTo(PaymentStatus.CANCELLED);
        assertThat(result.getBooking().getStatus()).isEqualTo(BookingStatus.PENDING);
        assertThat(result.getBooking().getBookingSeats()).allSatisfy(seat -> assertThat(seat.getShowSeat().getStatus()).isEqualTo(SeatStatus.PENDING));
        assertThat(payments.createPayment(CreatePaymentRequest.builder().bookingId(result.getBooking().getId()).method(PaymentMethods.BANK_TRANSFER).build(), username).getStatus()).isEqualTo(PaymentStatus.PENDING);
    }
    @Test void expiredReservationCannotBePaid() {
        var payment = payment();
        payment.getBooking().setExpiresAt(java.time.LocalDateTime.now().minusMinutes(1)); em.flush();
        assertThatThrownBy(() -> mock.confirm(payment.getId(), payment.getBooking().getUser().getUsername(), MockPaymentService.Outcome.SUCCESS)).isInstanceOf(IllegalStateException.class);
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PENDING);
    }
    @Test void anotherUserCannotConfirmPayment() {
        var payment = payment();
        assertThatThrownBy(() -> mock.confirm(payment.getId(), "missing-user", MockPaymentService.Outcome.SUCCESS)).isInstanceOf(AccessDeniedException.class);
    }
    @Test void disabledMockCannotConfirmEvenForOwner() {
        var payment = payment();
        var disabled = new MockPaymentService((PaymentServiceImpl) payments, false);
        assertThatThrownBy(() -> disabled.confirm(payment.getId(), payment.getBooking().getUser().getUsername(), MockPaymentService.Outcome.SUCCESS)).isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
    }
    @Test void cancellationCancelsPaymentAndReleasesSeats() {
        var payment = payment();
        var booking = bookings.cancelBooking(payment.getBooking().getId());
        assertThat(booking.getStatus()).isEqualTo(BookingStatus.CANCELLED);
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.CANCELLED);
        assertThat(booking.getBookingSeats()).allSatisfy(seat -> assertThat(seat.getShowSeat().getStatus()).isEqualTo(SeatStatus.AVAILABLE));
    }
}
