package com.example.ticketbox.payment.service;

import com.example.ticketbox.entity.*;
import com.example.ticketbox.enums.*;
import com.example.ticketbox.payment.repository.PaymentRepository;
import com.example.ticketbox.payment.sepay.*;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import java.util.regex.Pattern;

@Service @RequiredArgsConstructor
public class SepayPaymentService {
    private static final Pattern REFERENCE = Pattern.compile("(?<![A-Z0-9])TB(?:[A-F0-9]{20}|TEST[1-9][0-9]{0,17})(?![A-Z0-9])");
    private final SepaySettings settings;
    private final PaymentServiceImpl payments;
    private final PaymentRepository repository;
    private final SepayEventRepository events;
    private final EntityManager entityManager;

    public record Checkout(Long paymentId, String provider, boolean demo, String reference,
        BigDecimal amount, String currency, Instant expiresAt, String bank, String accountNo,
        String accountName, String qrImageUrl) {}

    @Transactional
    public Checkout checkout(Long id, String username) {
        if (!settings.isEnabled()) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "SePay is disabled");
        var payment = payments.lockOwnedPayment(id, username);
        if (payment.getStatus() != PaymentStatus.PENDING || payment.getBooking().getStatus() != BookingStatus.PENDING
                || payment.getBooking().getExpiresAt() == null || !payment.getBooking().getExpiresAt().isAfter(Instant.now()))
            throw new IllegalStateException("Reservation is no longer payable");
        if (payment.getCheckoutProvider() != null && !settings.provider().equals(payment.getCheckoutProvider()))
            throw new IllegalStateException("Payment belongs to another provider or SePay environment");
        if (payment.getSepayReference() == null) {
            payment.setSepayReference("TB" + UUID.randomUUID().toString().replace("-", "").substring(0, 20).toUpperCase(Locale.ROOT));
            payment.setCheckoutProvider(settings.provider());
        }
        if (payment.getAmount() == null || payment.getAmount().signum() <= 0 || payment.getAmount().stripTrailingZeros().scale() > 0)
            throw new IllegalStateException("SePay requires a positive whole-VND amount");
        String account = settings.getSubAccount().isBlank() ? settings.getAccountNumber() : settings.getSubAccount();
        String qr = settings.demo() ? null : SepayQrImage.forPayment(settings.getBank(), account, settings.getAccountName(), payment.getAmount(), payment.getSepayReference());
        return new Checkout(payment.getId(), settings.provider(), settings.demo(), payment.getSepayReference(),
            payment.getAmount(), "VND", payment.getBooking().getExpiresAt(), settings.getBank(), account, settings.getAccountName(), qr);
    }

    @Transactional
    public void receive(SepayPayload payload) {
        List<String> invalid = new ArrayList<>();
        if (payload.id() == null || payload.id() <= 0) invalid.add("id (positive number)");
        if (payload.transferAmount() == null || payload.transferAmount().signum() <= 0
                || payload.transferAmount().stripTrailingZeros().scale() > 0 || payload.transferAmount().precision() > 17)
            invalid.add("transferAmount (positive whole VND)");
        if (payload.accountNumber() == null || payload.accountNumber().isBlank()) invalid.add("accountNumber");
        if (payload.gateway() == null || payload.gateway().isBlank()) invalid.add("gateway");
        if (!("in".equals(payload.transferType()) || "out".equals(payload.transferType()))) invalid.add("transferType (in or out)");
        if (!invalid.isEmpty()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Missing or invalid fields: " + String.join(", ", invalid));
        String mode = settings.getMode().name();
        if (events.existsByModeAndTransactionId(mode, payload.id())) return;
        SepayEvent event = new SepayEvent();
        event.setMode(mode); event.setTransactionId(payload.id()); event.setAmount(payload.transferAmount());
        event.setOutcome("UNMATCHED_REFERENCE");
        if (!"in".equals(payload.transferType())) event.setOutcome("IGNORED_OUTGOING");
        else if (!settings.getAccountNumber().equals(payload.accountNumber())
                || !settings.getGateway().equalsIgnoreCase(payload.gateway())
                || (!settings.getSubAccount().isBlank() && !settings.getSubAccount().equals(payload.subAccount())))
            event.setOutcome("ACCOUNT_MISMATCH");
        else {
            Set<String> references = new HashSet<>();
            for (String text : new String[]{payload.code(), payload.content()}) {
                if (text == null) continue;
                var matcher = REFERENCE.matcher(text.toUpperCase(Locale.ROOT));
                while (matcher.find()) references.add(matcher.group());
            }
            if (references.size() == 1) {
                String reference = references.iterator().next(); event.setReference(reference);
                boolean legacyDemo = reference.startsWith("TBTEST");
                var found = legacyDemo ? repository.findById(Long.parseLong(reference.substring(6)))
                    : repository.findBySepayReference(reference);
                if (found.isPresent()) {
                    Long paymentId = found.get().getId();
                    payments.lockBookingForPayment(paymentId);
                    var payment = repository.findByIdForUpdate(paymentId).orElseThrow(); entityManager.refresh(payment);
                    // Serialize retry processing with all other actions on this booking.
                    if (events.existsByModeAndTransactionId(mode, payload.id())) return;
                    event.setPaymentId(paymentId);
                    if (legacyDemo) event.setOutcome("LEGACY_DEMO_REFERENCE");
                    else if (!settings.provider().equals(payment.getCheckoutProvider())) event.setOutcome("ENVIRONMENT_MISMATCH");
                    else if (payload.transferAmount().compareTo(payment.getAmount()) != 0) event.setOutcome("AMOUNT_MISMATCH");
                    else if (payment.getStatus() != PaymentStatus.PENDING || payment.getBooking().getStatus() != BookingStatus.PENDING)
                        event.setOutcome("PAYMENT_NOT_PENDING");
                    else if (payment.getBooking().getExpiresAt() == null || !payment.getBooking().getExpiresAt().isAfter(Instant.now()))
                        event.setOutcome("LATE_TRANSFER");
                    else {
                        // Persist deduplication in the same transaction as the payment and seat changes.
                        event.setOutcome("CONFIRMED"); events.saveAndFlush(event);
                        payments.settlePayment(payment, "SEPAY-" + mode + "-" + payload.id());
                        return;
                    }
                }
            } else if (references.size() > 1) event.setOutcome("AMBIGUOUS_REFERENCE");
        }
        events.saveAndFlush(event);
    }
}
