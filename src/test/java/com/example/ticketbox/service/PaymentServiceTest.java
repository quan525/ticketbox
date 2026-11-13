package com.example.ticketbox.service;

import com.example.ticketbox.TestDataFactory;
import com.example.ticketbox.booking.dto.request.CreateBookingReq;
import com.example.ticketbox.booking.service.BookingService;
import com.example.ticketbox.booking.repository.BookingRepository;
import com.example.ticketbox.entity.Booking;
import com.example.ticketbox.entity.Movies;
import com.example.ticketbox.entity.Payments;
import com.example.ticketbox.entity.Rooms;
import com.example.ticketbox.entity.Show;
import com.example.ticketbox.entity.ShowSeat;
import com.example.ticketbox.entity.Users;
import com.example.ticketbox.enums.BookingStatus;
import com.example.ticketbox.enums.PaymentMethods;
import com.example.ticketbox.enums.PaymentStatus;
import com.example.ticketbox.enums.SeatStatus;
import com.example.ticketbox.enums.UserRoles;
import com.example.ticketbox.payment.dto.request.CreatePaymentRequest;
import com.example.ticketbox.payment.service.PaymentService;
import com.example.ticketbox.show.dto.Request.CreateShowReq;
import com.example.ticketbox.showseat.service.ShowSeatServiceImpl;
import com.example.ticketbox.user.dto.request.CreateUserRequest;
import com.example.ticketbox.user.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.annotation.DirtiesContext;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@org.springframework.transaction.annotation.Transactional
class PaymentServiceTest {

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private BookingService bookingService;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private TestDataFactory dataFactory;

    @Autowired
    private ShowSeatServiceImpl showSeatService;

    @Test
    void createPayment_usesServerCalculatedBookingAmount() {
        Booking booking = createBooking();

        Payments payment = createPayment(booking);

        assertNotNull(payment.getId());
        assertEquals(PaymentStatus.PENDING, payment.getStatus());
        assertEquals(PaymentMethods.CREDIT_CARD, payment.getMethod());
        assertEquals(0, booking.getTotalPrice().compareTo(payment.getAmount()));
        assertNull(payment.getTransactionId());
        assertNull(payment.getPaidAt());
    }

    @Test
    void createPayment_rejectsSecondActivePayment() {
        Booking booking = createBooking();
        createPayment(booking);

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> createPayment(booking));

        assertEquals("Booking already has an active payment", exception.getMessage());
    }

    @Test
    void completePayment_confirmsBookingAndBooksSeats() {
        Booking booking = createBooking();
        Payments payment = createPayment(booking);
        Users operator = userService.createUser(CreateUserRequest.builder()
                .username("payment-operator")
                .password("password")
                .build());
        userService.updateUserRole(operator.getId(), UserRoles.ADMIN);

        Payments completed = paymentService.completePayment(
                payment.getId(), " gateway-transaction-1 ", operator.getUsername());

        assertEquals(PaymentStatus.PAID, completed.getStatus());
        assertEquals("gateway-transaction-1", completed.getTransactionId());
        assertNotNull(completed.getPaidAt());
        assertEquals(BookingStatus.CONFIRMED, completed.getBooking().getStatus());
        assertTrue(completed.getBooking().getBookingSeats().stream()
                .allMatch(seat -> seat.getShowSeat().getStatus() == SeatStatus.BOOKED));
        assertTrue(completed.getBooking().getBookingSeats().stream()
                .allMatch(seat -> seat.getShowSeat().getHeldUntil() == null));
    }

    @Test
    void cancelPayment_allowsAReplacementPayment() {
        Booking booking = createBooking();
        Payments first = createPayment(booking);

        Payments cancelled = paymentService.cancelPayment(
                first.getId(), booking.getUser().getUsername());
        Payments replacement = createPayment(booking);

        assertEquals(PaymentStatus.CANCELLED, cancelled.getStatus());
        assertEquals(PaymentStatus.PENDING, replacement.getStatus());
        assertNotEquals(first.getId(), replacement.getId());
    }

    @Test
    void paymentAccess_rejectsAnotherUser() {
        Booking booking = createBooking();
        Payments payment = createPayment(booking);
        Users anotherUser = userService.createUser(CreateUserRequest.builder()
                .username("another-payment-user")
                .password("password")
                .build());

        assertThrows(AccessDeniedException.class,
                () -> paymentService.getPayment(payment.getId(), anotherUser.getUsername()));
    }

    @Test
    void createPayment_rejectsExpiredBooking() {
        Booking booking = createBooking();
        booking.setExpiresAt(java.time.Instant.now().minusSeconds(1));
        bookingRepository.save(booking);

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> createPayment(booking));

        assertEquals("Booking has expired", exception.getMessage());
    }

    private Payments createPayment(Booking booking) {
        return paymentService.createPayment(
                CreatePaymentRequest.builder()
                        .bookingId(booking.getId())
                        .method(PaymentMethods.CREDIT_CARD)
                        .build(),
                booking.getUser().getUsername());
    }

    private Booking createBooking() {
        Users user = dataFactory.createUser();
        Rooms room = dataFactory.createRoom();
        dataFactory.createSeatsForRoom(room);
        Movies movie = dataFactory.createMovie();
        Show show = dataFactory.createShow(CreateShowReq.builder()
                .roomId(room.getId())
                .movieId(movie.getId())
                .build());
        showSeatService.generateForShow(show.getId());
        List<Integer> showSeatIds = showSeatService.getShowSeatsByShowId(show.getId())
                .stream()
                .limit(2)
                .map(ShowSeat::getId)
                .toList();

        return bookingService.createBooking(CreateBookingReq.builder()
                .userId(user.getId())
                .showId(show.getId())
                .showSeatIds(showSeatIds)
                .build());
    }
}
