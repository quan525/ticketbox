package com.example.ticketbox.movie.dto.response;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MovieResponse {

    private Integer id;
    private String title;
    private String author;
    private String description;
    private Integer durationMin;
    private String language;
}
