package com.example.ticketbox.payment.service;

import com.example.ticketbox.entity.Payments;
import com.example.ticketbox.enums.BookingStatus;
import com.example.ticketbox.enums.PaymentStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Local simulation with a plain-text QR. */
@Service
public class MockPaymentService {
    private final PaymentServiceImpl payments;
    private final boolean enabled;
    private final com.example.ticketbox.payment.sepay.SepaySettings sepay;

    @org.springframework.beans.factory.annotation.Autowired
    public MockPaymentService(PaymentServiceImpl payments, @Value("${payments.mock.enabled:false}") boolean enabled, com.example.ticketbox.payment.sepay.SepaySettings sepay) {
        this.payments = payments;
        this.enabled = enabled;
        this.sepay = sepay;
    }

    public MockPaymentService(PaymentServiceImpl payments, boolean enabled) {
        this(payments, enabled, new com.example.ticketbox.payment.sepay.SepaySettings());
    }

    public boolean enabled() { return enabled && !sepay.isEnabled(); }
    public record Checkout(Long paymentId, String reference, BigDecimal amount, String currency,
                           LocalDateTime expiresAt, String qrPayload, boolean demo,
                           String bankBin, String accountNo, String accountName, String qrImageUrl, boolean paymentDetailsIncluded) {}

    @Transactional
    public Checkout checkout(Long id, String username) {
        requireEnabled();
        var payment = payments.lockOwnedPayment(id, username);
        if (payment.getCheckoutProvider() != null && payment.getCheckoutProvider().startsWith("SEPAY"))
            throw new IllegalStateException("This payment belongs to SePay");
        requirePending(payment);
        String reference = "TBTEST" + payment.getId();
        return new Checkout(payment.getId(), reference, payment.getAmount(), "VND",
                payment.getBooking().getExpiresAt(), "Ticketbox demo " + reference, true,
                null, null, null, null, false);
    }

    @Transactional
    public Payments confirm(Long id, String username, Outcome outcome) {
        requireEnabled();
        var payment = payments.lockOwnedPayment(id, username);
        if (outcome == Outcome.SUCCESS && payment.getStatus() == PaymentStatus.PAID
                && reference(payment).equals(payment.getTransactionId())) return payment;
        if (outcome == Outcome.DECLINED && payment.getStatus() == PaymentStatus.CANCELLED) return payment;
        if (payment.getCheckoutProvider() != null && payment.getCheckoutProvider().startsWith("SEPAY"))
            throw new IllegalStateException("This payment belongs to SePay");
        requirePending(payment);
        if (outcome == Outcome.DECLINED) {
            // Keep the reservation's seats held so a customer can try another payment.
            payment.setStatus(PaymentStatus.CANCELLED);
            return payment;
        }
        return payments.settlePayment(payment, reference(payment));
    }

    public enum Outcome { SUCCESS, DECLINED }
    private static String reference(Payments payment) { return "MOCK-TICKETBOX-" + payment.getId(); }
    private void requireEnabled() {
        if (!enabled()) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Mock payments are disabled");
    }
    private static void requirePending(Payments payment) {
        if (payment.getStatus() != PaymentStatus.PENDING || payment.getBooking().getStatus() != BookingStatus.PENDING) {
            throw new IllegalStateException("Only pending reservations can use demo checkout");
        }
        if (payment.getBooking().getExpiresAt() == null || !payment.getBooking().getExpiresAt().isAfter(LocalDateTime.now())) {
            throw new IllegalStateException("Booking has expired");
        }
    }
}
