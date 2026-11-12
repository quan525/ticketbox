package com.example.ticketbox.payment.service;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

/** VietQR/EMV payload encoding for local format tests, never a live recipient. */
public final class VietQrTestPayload {
    public static final String BANK_BIN = "000000";
    public static final String ACCOUNT_NO = "000000000000";
    private VietQrTestPayload() {}

    public static String forPayment(Long id, BigDecimal amount) {
        return encode(BANK_BIN, ACCOUNT_NO, amount, "TBTEST" + id);
    }

    static String encode(String bankBin, String account, BigDecimal amount, String reference) {
        if (!bankBin.matches("[0-9]{6}") || !account.matches("[A-Za-z0-9]{1,19}")
                || amount == null || amount.signum() <= 0 || reference == null || !reference.matches("[A-Za-z0-9 ]{1,25}")) {
            throw new IllegalArgumentException("Invalid VietQR test data");
        }
        String value = amount.toBigIntegerExact().toString();
        if (value.length() > 13) throw new IllegalArgumentException("VietQR amount is too large");
        String beneficiary = field("00", bankBin) + field("01", account);
        String merchant = field("00", "A000000727") + field("01", beneficiary) + field("02", "QRIBFTTA");
        String payload = field("00", "01") + field("01", "12") + field("38", merchant)
                + field("53", "704") + field("54", value) + field("58", "VN")
                + field("62", field("08", reference)) + "6304";
        return payload + crc(payload);
    }

    private static String field(String id, String value) {
        int length = value.getBytes(StandardCharsets.UTF_8).length;
        if (length > 99) throw new IllegalArgumentException("VietQR field is too long");
        return id + String.format(Locale.ROOT, "%02d", length) + value;
    }

    static String crc(String text) {
        int checksum = 0xFFFF;
        for (byte value : text.getBytes(StandardCharsets.UTF_8)) {
            checksum ^= (value & 0xFF) << 8;
            for (int bit = 0; bit < 8; bit++) {
                checksum = ((checksum & 0x8000) != 0 ? (checksum << 1) ^ 0x1021 : checksum << 1) & 0xFFFF;
            }
        }
        return String.format(Locale.ROOT, "%04X", checksum);
    }
}
