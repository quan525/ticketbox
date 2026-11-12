package com.example.ticketbox.payment.service;

import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import static org.assertj.core.api.Assertions.*;

class VietQrTestPayloadTest {
    @Test void matchesPublishedDynamicVietQrExample() {
        // Reference vector from xuannghia/vietnam-qr-pay's documented encoder example.
        assertThat(VietQrTestPayload.encode("970416", "257678859", new BigDecimal("10000"), "Chuyen tien"))
                .isEqualTo("00020101021238530010A0000007270123000697041601092576788590208QRIBFTTA53037045405100005802VN62150811Chuyen tien630453E6");
    }
    @Test void crcMatchesStandardCheckVector() {
        assertThat(VietQrTestPayload.crc("123456789")).isEqualTo("29B1");
    }
    @Test void encodesTestRecipientAndExactVndAmount() {
        var payload = VietQrTestPayload.forPayment(36L, new BigDecimal("300000.00"));
        assertThat(payload).contains("0006000000", "0112000000000000", "5406300000", "5802VN", "TBTEST36");
        assertThat(payload.substring(payload.length() - 4)).isEqualTo(VietQrTestPayload.crc(payload.substring(0, payload.length() - 4)));
    }
    @Test void rejectsFractionalOrNegativeVnd() {
        assertThatThrownBy(() -> VietQrTestPayload.forPayment(1L, new BigDecimal("0.5"))).isInstanceOf(ArithmeticException.class);
        assertThatThrownBy(() -> VietQrTestPayload.forPayment(1L, new BigDecimal("-1"))).isInstanceOf(IllegalArgumentException.class);
    }
}
