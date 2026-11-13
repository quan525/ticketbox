package com.example.ticketbox.booking.service;

import com.example.ticketbox.booking.dto.BookingMapper;
import com.example.ticketbox.booking.dto.request.CreateBookingReq;
import com.example.ticketbox.booking.repository.BookingRepository;
import com.example.ticketbox.entity.*;
import com.example.ticketbox.enums.BookingStatus;
import com.example.ticketbox.show.service.ShowService;
import com.example.ticketbox.showseat.repository.ShowSeatRepository;
import com.example.ticketbox.user.service.UserService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class BookingServiceImpl implements BookingService {

    private final BookingRepository bookingRepo;
    private final UserService userService;
    private final ShowService showService;
    private final ShowSeatRepository showSeatRepo;
    private final BookingMapper mapper;
    private final jakarta.persistence.EntityManager entityManager;
    private final com.example.ticketbox.payment.repository.PaymentRepository payments;

    @Override
    @Transactional
    public Booking createBooking(CreateBookingReq req) {
        Users user = userService.getUserById(req.getUserId());
        Show show = showService.getShowById(req.getShowId());
        if (show.getMovie().isArchived()) throw new IllegalStateException("This movie is archived and is not accepting reservations");

        if (req.getShowSeatIds() == null || req.getShowSeatIds().isEmpty()) {
            throw new IllegalArgumentException("At least one show seat is required");
        }

        List<Integer> distinctSeatIds = req.getShowSeatIds().stream().distinct().toList();
        if (distinctSeatIds.size() != req.getShowSeatIds().size()) {
            throw new IllegalArgumentException("Duplicate show seats are not allowed");
        }

        List<ShowSeat> showSeats = showSeatRepo.findByIdInForUpdate(distinctSeatIds);
        showSeats.forEach(entityManager::refresh);

        if (showSeats.size() != distinctSeatIds.size()) {
            throw new EntityNotFoundException("One or more show seats not found");
        }

        boolean hasSeatFromAnotherShow = showSeats.stream()
                .anyMatch(showSeat -> !showSeat.getShow().getId().equals(show.getId()));
        if (hasSeatFromAnotherShow) {
            throw new IllegalArgumentException("All seats must belong to the selected show");
        }

        boolean hasUnavailableSeat = showSeats.stream()
                .peek(showSeat -> {
                    if (showSeat.getStatus() == com.example.ticketbox.enums.SeatStatus.PENDING && showSeat.getHeldUntil() != null && !showSeat.getHeldUntil().isAfter(Instant.now())) {
                        showSeat.setStatus(com.example.ticketbox.enums.SeatStatus.AVAILABLE);
                        showSeat.setHeldUntil(null);
                    }
                })
                .anyMatch(showSeat -> showSeat.getStatus() != com.example.ticketbox.enums.SeatStatus.AVAILABLE);
        if (hasUnavailableSeat) {
            throw new IllegalStateException("One or more seats are not available");
        }

        BigDecimal totalPrice = BigDecimal.ZERO;
        Booking booking = mapper.toEntity(req);
        booking.setUser(user);
        booking.setShow(show);
        booking.setStatus(BookingStatus.PENDING);
        booking.setExpiresAt(Instant.now().truncatedTo(ChronoUnit.MICROS).plus(10, ChronoUnit.MINUTES));

        for (ShowSeat ss : showSeats) {
            ss.setStatus(com.example.ticketbox.enums.SeatStatus.PENDING);
            ss.setHeldUntil(booking.getExpiresAt());
            booking.getBookingSeats().add(
                    BookingSeat.builder()
                            .booking(booking)
                            .showSeat(ss)
                            .price(ss.getPrice())
                            .build()
            );

            totalPrice = totalPrice.add(ss.getPrice());
        }

        booking.setTotalPrice(totalPrice);
        return bookingRepo.save(booking);
    }

    @Override
    @Transactional(readOnly = true)
    public Booking getBooking(Long id) {
        return bookingRepo.findById(id)
                .orElseThrow(() ->
                        new EntityNotFoundException("Booking not found"));
    }

    @Override
    public List<Booking> getBookings() {
        return bookingRepo.findAll();
    }

    @Override
    public List<Booking> getBookingsByUser(Integer userId) {
        return bookingRepo.findByUserId(userId);
    }

    @Override
    public Booking cancelBooking(Long id) {
        Booking booking = bookingRepo.findByIdForUpdate(id)
                .orElseThrow(() -> new EntityNotFoundException("Booking not found"));
        entityManager.refresh(booking);

        if (booking.getStatus() == BookingStatus.CONFIRMED) {
            throw new IllegalStateException("Cannot cancel confirmed booking");
        }

        if (booking.getStatus() == BookingStatus.CANCELLED) {
            return booking;
        }

        booking.setStatus(BookingStatus.CANCELLED);
        payments.findByBookingIdForUpdate(id).forEach(payment -> {
            entityManager.refresh(payment);
            if (payment.getStatus() == com.example.ticketbox.enums.PaymentStatus.PENDING) {
                payment.setStatus(com.example.ticketbox.enums.PaymentStatus.CANCELLED);
            }
        });
        var seatIds = booking.getBookingSeats().stream().map(seat -> seat.getShowSeat().getId()).toList();
        showSeatRepo.findByIdInForUpdate(seatIds).forEach(showSeat -> {
            entityManager.refresh(showSeat);
            if (showSeat.getStatus() == com.example.ticketbox.enums.SeatStatus.PENDING && java.util.Objects.equals(showSeat.getHeldUntil(), booking.getExpiresAt())) {
                showSeat.setStatus(com.example.ticketbox.enums.SeatStatus.AVAILABLE);
                showSeat.setHeldUntil(null);
            }
        });
        return booking;
    }
}
