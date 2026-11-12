package com.example.ticketbox.payment.sepay;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;

@Component @RequiredArgsConstructor
public class SepayAuthentication {
    private final SepaySettings settings;
    public void verify(HttpHeaders headers, byte[] body) {
        if (!settings.isEnabled()) throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "SePay processing is disabled. Configure SePay locally and restart Ticketbox.");
        boolean valid = false;
        if (settings.getAuth() == SepaySettings.Auth.API_KEY) {
            valid = same("Apikey " + settings.getWebhookSecret(), headers.getFirst(HttpHeaders.AUTHORIZATION));
        } else {
            String timestamp = headers.getFirst("X-SePay-Timestamp");
            String signature = headers.getFirst("X-SePay-Signature");
            try {
                long seconds = Long.parseLong(timestamp);
                long now = Instant.now().getEpochSecond();
                if (seconds >= now - 300 && seconds <= now + 300) {
                    Mac mac = Mac.getInstance("HmacSHA256");
                    mac.init(new SecretKeySpec(settings.getWebhookSecret().getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
                    mac.update((timestamp + ".").getBytes(StandardCharsets.UTF_8));
                    valid = same("sha256=" + HexFormat.of().formatHex(mac.doFinal(body)), signature);
                }
            } catch (java.security.GeneralSecurityException | IllegalArgumentException exception) {
                valid = false;
            }
        }
        if (!valid) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid SePay webhook authentication");
    }
    private boolean same(String expected, String supplied) {
        return supplied != null && MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8), supplied.getBytes(StandardCharsets.UTF_8));
    }
}
