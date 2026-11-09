package com.example.ticketbox.show.dto.Response;

import com.example.ticketbox.movie.dto.response.MovieResponse;
import com.example.ticketbox.room.dto.response.RoomResponse;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShowResponse {

    private Integer id;

    private MovieResponse movie;

    private RoomResponse room;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}