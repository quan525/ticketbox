package com.example.ticketbox.booking.dto.response;

import com.example.ticketbox.enums.BookingStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class BookingResponse {

    private Long id;
    private Integer userId;
    private Integer showId;
    private BookingStatus status;
    private BigDecimal totalPrice;
    private LocalDateTime expiresAt;
    private String movieTitle;
    private String roomName;
    private LocalDateTime startTime;
    private java.util.List<String> seats;
}
