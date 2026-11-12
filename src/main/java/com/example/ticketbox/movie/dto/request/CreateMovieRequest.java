package com.example.ticketbox.movie.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateMovieRequest {

    @NotBlank
    @jakarta.validation.constraints.Size(max = 255)
    private String title;

    @NotBlank
    @jakarta.validation.constraints.Size(max = 255)
    private String author;

    private String description;

    @NotNull
    @Positive
    private Integer durationMin;

    @NotBlank
    @jakarta.validation.constraints.Size(max = 10)
    @Builder.Default
    private String language = "EN";
}
