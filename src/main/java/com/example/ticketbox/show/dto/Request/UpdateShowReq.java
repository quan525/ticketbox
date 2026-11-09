package com.example.ticketbox.show.dto.Request;

import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateShowReq {
    @Positive
    private Integer movieId;

    @Positive
    private Integer roomId;

    private LocalDateTime startTime;

    private LocalDateTime endTime;
}
