package com.example.ticketbox.exception;

public record ErrorResponse(
        int status,
        String message
) {
}
