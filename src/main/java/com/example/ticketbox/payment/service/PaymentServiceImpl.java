package com.example.ticketbox.payment.service;

import com.example.ticketbox.booking.repository.BookingRepository;
import com.example.ticketbox.entity.Booking;
import com.example.ticketbox.entity.Payments;
import com.example.ticketbox.entity.Users;
import com.example.ticketbox.enums.BookingStatus;
import com.example.ticketbox.enums.PaymentStatus;
import com.example.ticketbox.enums.SeatStatus;
import com.example.ticketbox.enums.UserRoles;
import com.example.ticketbox.payment.dto.PaymentMapper;
import com.example.ticketbox.payment.dto.request.CreatePaymentRequest;
import com.example.ticketbox.payment.repository.PaymentRepository;
import com.example.ticketbox.user.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class PaymentServiceImpl implements PaymentService {
    private static final List<PaymentStatus> ACTIVE_STATUSES =
            List.of(PaymentStatus.PENDING, PaymentStatus.PAID);

    private final PaymentRepository paymentRepository;
    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;
    private final PaymentMapper paymentMapper;
    private final PaymentMethodFactory paymentMethodFactory;
    private final jakarta.persistence.EntityManager entityManager;
    private final com.example.ticketbox.showseat.repository.ShowSeatRepository showSeats;

    @Override
    public Payments createPayment(CreatePaymentRequest request, String username) {
        Booking booking = getBookingForUpdate(request.getBookingId());
        requireAccess(booking, username, com.example.ticketbox.enums.UserPermission.MANAGE_PAYMENTS);
        requirePayable(booking);

        if (paymentRepository.existsByBookingIdAndStatusIn(booking.getId(), ACTIVE_STATUSES)) {
            throw new IllegalStateException("Booking already has an active payment");
        }

        Payments payment = paymentMapper.toEntity(request);
        payment.setBooking(booking);
        payment.setMethod(paymentMethodFactory.requireSupported(request.getMethod()));
        payment.setAmount(booking.getTotalPrice());
        payment.setStatus(PaymentStatus.PENDING);
        payment.setTransactionId(null);
        payment.setPaidAt(null);
        return paymentRepository.save(payment);
    }

    @Override
    @Transactional(readOnly = true)
    public Payments getPayment(Long id, String username) {
        Payments payment = findPayment(id);
        requireAccess(payment.getBooking(), username, com.example.ticketbox.enums.UserPermission.VIEW_PAYMENTS);
        return payment;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Payments> getPaymentsByBooking(Long bookingId, String username) {
        Booking booking = getBooking(bookingId);
        requireAccess(booking, username, com.example.ticketbox.enums.UserPermission.VIEW_PAYMENTS);
        return paymentRepository.findByBookingIdOrderByCreatedAtDesc(bookingId);
    }

    @Override
    public Payments completePayment(Long id, String transactionId, String username) {
        lockBookingForPayment(id);
        Payments payment = findPaymentForUpdate(id);
        requirePrivileged(username);

        return settlePayment(payment, transactionId);
    }

    Payments settlePayment(Payments payment, String transactionId) {

        String normalizedTransactionId = transactionId == null ? "" : transactionId.trim();
        if (normalizedTransactionId.isEmpty()) {
            throw new IllegalArgumentException("Transaction ID is required");
        }
        if (payment.getStatus() == PaymentStatus.PAID
                && normalizedTransactionId.equals(payment.getTransactionId())) {
            return payment;
        }

        if (payment.getStatus() != PaymentStatus.PENDING) {
            throw new IllegalStateException("Only pending payments can be completed");
        }
        requirePayable(payment.getBooking());

        if (paymentRepository.existsByTransactionId(normalizedTransactionId)) {
            throw new IllegalStateException("Transaction ID already exists");
        }

        Booking booking = payment.getBooking();
        var seatIds = booking.getBookingSeats().stream().map(seat -> seat.getShowSeat().getId()).toList();
        var lockedSeats = showSeats.findByIdInForUpdate(seatIds);
        lockedSeats.forEach(entityManager::refresh);
        requirePayable(booking);
        if (lockedSeats.size() != seatIds.size() || lockedSeats.stream().anyMatch(seat -> seat.getStatus() != SeatStatus.PENDING
                || !java.util.Objects.equals(seat.getHeldUntil(), booking.getExpiresAt()))) {
            throw new IllegalStateException("The booking no longer holds all its seats");
        }
        payment.setStatus(PaymentStatus.PAID);
        payment.setTransactionId(normalizedTransactionId);
        payment.setPaidAt(LocalDateTime.now());

        booking.setStatus(BookingStatus.CONFIRMED);
        lockedSeats.forEach(seat -> {
            seat.setStatus(SeatStatus.BOOKED);
            seat.setHeldUntil(null);
        });
        bookingRepository.save(booking);
        return payment;
    }

    @Override
    public Payments cancelPayment(Long id, String username) {
        lockBookingForPayment(id);
        Payments payment = findPaymentForUpdate(id);
        requireAccess(payment.getBooking(), username, com.example.ticketbox.enums.UserPermission.MANAGE_PAYMENTS);
        if (payment.getStatus() != PaymentStatus.PENDING) {
            throw new IllegalStateException("Only pending payments can be cancelled");
        }
        payment.setStatus(PaymentStatus.CANCELLED);
        return payment;
    }

    private Payments findPayment(Long id) {
        return paymentRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Payment not found"));
    }

    private Payments findPaymentForUpdate(Long id) {
        var payment = paymentRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new EntityNotFoundException("Payment not found"));
        entityManager.refresh(payment);
        return payment;
    }

    private Booking getBooking(Long id) {
        return bookingRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Booking not found"));
    }

    private Booking getBookingForUpdate(Long id) {
        var booking = bookingRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new EntityNotFoundException("Booking not found"));
        entityManager.refresh(booking);
        return booking;
    }

    void lockBookingForPayment(Long paymentId) {
        getBookingForUpdate(paymentRepository.findBookingId(paymentId)
                .orElseThrow(() -> new EntityNotFoundException("Payment not found")));
    }

    Payments lockOwnedPayment(Long id, String username) {
        lockBookingForPayment(id);
        Payments payment = findPaymentForUpdate(id);
        requireAccess(payment.getBooking(), username, com.example.ticketbox.enums.UserPermission.MANAGE_PAYMENTS);
        return payment;
    }

    private void requirePayable(Booking booking) {
        if (booking.getStatus() != BookingStatus.PENDING) {
            throw new IllegalStateException("Only pending bookings can be paid");
        }
        if (booking.getExpiresAt() != null && !booking.getExpiresAt().isAfter(Instant.now())) {
            throw new IllegalStateException("Booking has expired");
        }
    }

    private void requireAccess(Booking booking, String username, com.example.ticketbox.enums.UserPermission permission) {
        Users currentUser = userRepository.findByUsername(username)
                .orElseThrow(() -> new AccessDeniedException("Authenticated user not found"));
        boolean privileged = currentUser.hasPermission(permission);
        if (!privileged && !currentUser.getId().equals(booking.getUser().getId())) {
            throw new AccessDeniedException("You cannot access this payment");
        }
    }

    private void requirePrivileged(String username) {
        Users currentUser = userRepository.findByUsername(username)
                .orElseThrow(() -> new AccessDeniedException("Authenticated user not found"));
        if (!currentUser.hasPermission(com.example.ticketbox.enums.UserPermission.MANAGE_PAYMENTS)) {
            throw new AccessDeniedException("Only a payment operator can complete payments");
        }
    }
}
