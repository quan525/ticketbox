package com.example.ticketbox.movie.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateMovieRequest {

    private String title;

    private String author;

    private String description;

    private Integer durationMin;

    @NotBlank
    @Builder.Default
    private String language = "EN";
}
