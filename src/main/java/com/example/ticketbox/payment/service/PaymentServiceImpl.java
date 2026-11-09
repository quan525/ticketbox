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

    @Override
    public Payments createPayment(CreatePaymentRequest request, String username) {
        Booking booking = getBookingForUpdate(request.getBookingId());
        requireAccess(booking, username);
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
        requireAccess(payment.getBooking(), username);
        return payment;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Payments> getPaymentsByBooking(Long bookingId, String username) {
        Booking booking = getBooking(bookingId);
        requireAccess(booking, username);
        return paymentRepository.findByBookingIdOrderByCreatedAtDesc(bookingId);
    }

    @Override
    public Payments completePayment(Long id, String transactionId, String username) {
        Payments payment = findPaymentForUpdate(id);
        requirePrivileged(username);

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

        payment.setStatus(PaymentStatus.PAID);
        payment.setTransactionId(normalizedTransactionId);
        payment.setPaidAt(LocalDateTime.now());

        Booking booking = payment.getBooking();
        booking.setStatus(BookingStatus.CONFIRMED);
        booking.getBookingSeats().forEach(bookingSeat -> {
            bookingSeat.getShowSeat().setStatus(SeatStatus.BOOKED);
            bookingSeat.getShowSeat().setHeldUntil(null);
        });
        bookingRepository.save(booking);
        return payment;
    }

    @Override
    public Payments cancelPayment(Long id, String username) {
        Payments payment = findPayment(id);
        requireAccess(payment.getBooking(), username);
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
        return paymentRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new EntityNotFoundException("Payment not found"));
    }

    private Booking getBooking(Long id) {
        return bookingRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Booking not found"));
    }

    private Booking getBookingForUpdate(Long id) {
        return bookingRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new EntityNotFoundException("Booking not found"));
    }

    private void requirePayable(Booking booking) {
        if (booking.getStatus() != BookingStatus.PENDING) {
            throw new IllegalStateException("Only pending bookings can be paid");
        }
        if (booking.getExpiresAt() != null && !booking.getExpiresAt().isAfter(LocalDateTime.now())) {
            throw new IllegalStateException("Booking has expired");
        }
    }

    private void requireAccess(Booking booking, String username) {
        Users currentUser = userRepository.findByUsername(username)
                .orElseThrow(() -> new AccessDeniedException("Authenticated user not found"));
        boolean privileged = currentUser.getRole() == UserRoles.ADMIN
                || currentUser.getRole() == UserRoles.MODERATOR;
        if (!privileged && !currentUser.getId().equals(booking.getUser().getId())) {
            throw new AccessDeniedException("You cannot access this payment");
        }
    }

    private void requirePrivileged(String username) {
        Users currentUser = userRepository.findByUsername(username)
                .orElseThrow(() -> new AccessDeniedException("Authenticated user not found"));
        if (currentUser.getRole() != UserRoles.ADMIN
                && currentUser.getRole() != UserRoles.MODERATOR) {
            throw new AccessDeniedException("Only a payment operator can complete payments");
        }
    }
}
