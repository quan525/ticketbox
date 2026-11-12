package com.example.ticketbox.payment.sepay;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter @Setter @Component
@ConfigurationProperties(prefix = "payments.sepay")
public class SepaySettings {
    public enum Mode { TEST, LIVE }
    public enum Auth { HMAC, API_KEY }
    private boolean enabled;
    private Mode mode = Mode.TEST;
    private Auth auth = Auth.HMAC;
    private String webhookSecret = "";
    private String bank = "";
    private String gateway = "";
    private String accountNumber = "";
    private String accountName = "";
    private String subAccount = "";

    @PostConstruct
    void validate() {
        if (!enabled) return;
        if (mode == null || auth == null || webhookSecret.length() < 16
                || !bank.matches("[A-Za-z0-9_-]{2,50}") || gateway.isBlank()
                || !accountNumber.matches("[A-Za-z0-9]{1,40}") || accountName.isBlank())
            throw new IllegalStateException("Complete the private SePay settings (mode, authentication, secret of 16+ characters, bank, gateway, parent account and holder) before enabling SePay");
    }
    public boolean demo() { return mode == Mode.TEST; }
    public String provider() { return "SEPAY_" + mode.name(); }
}
