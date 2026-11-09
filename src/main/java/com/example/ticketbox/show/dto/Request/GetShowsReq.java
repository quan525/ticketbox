package com.example.ticketbox.show.dto.Request;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class GetShowsReq {
//    @Positive
//    private Integer movieId;
//
//    @Positive
//    private Integer roomId;

    private String movieTitle;

    private LocalDateTime startTime;

//    private LocalDateTime endTime;
}
