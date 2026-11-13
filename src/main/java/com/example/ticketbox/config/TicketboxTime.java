package com.example.ticketbox.config;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

public final class TicketboxTime {
    public static final ZoneId CINEMA_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private TicketboxTime() {}

    // Existing reservation deadlines are stored as server-local timestamps.
    public static Instant reservationDeadline(LocalDateTime deadline) {
        return deadline == null ? null : deadline.atZone(ZoneId.systemDefault()).toInstant();
    }
}
