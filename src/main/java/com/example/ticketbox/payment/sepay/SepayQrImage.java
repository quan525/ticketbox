package com.example.ticketbox.payment.sepay;

import java.math.BigDecimal;
import org.springframework.web.util.UriComponentsBuilder;

public final class SepayQrImage {
    private SepayQrImage() {}
    public static String forPayment(String bank, String account, String holder, BigDecimal amount, String reference) {
        if (amount == null || amount.signum() <= 0 || amount.stripTrailingZeros().scale() > 0)
            throw new IllegalStateException("QR checkout requires a positive whole-VND amount");
        return UriComponentsBuilder.fromUriString("https://vietqr.app/img")
            .queryParam("bank", bank).queryParam("acc", account)
            .queryParam("template", "compact").queryParam("showinfo", "true").queryParam("holder", holder)
            .queryParam("amount", amount.toBigIntegerExact().toString()).queryParam("des", reference)
            .build().encode().toUriString();
    }
}
