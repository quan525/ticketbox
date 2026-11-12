package com.example.ticketbox.payment.sepay;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;

// Explicit names: SePay sends camelCase while Ticketbox's own API uses snake_case.
@com.fasterxml.jackson.annotation.JsonIgnoreProperties(ignoreUnknown = true)
public record SepayPayload(Long id, String gateway,
    @JsonProperty("accountNumber") String accountNumber,
    @JsonProperty("subAccount") String subAccount,
    String code, String content,
    @JsonProperty("transferType") String transferType,
    @JsonProperty("transferAmount") BigDecimal transferAmount) {}
