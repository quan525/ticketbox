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

    @jakarta.validation.constraints.Pattern(regexp = "(?s).*\\S.*")
    @jakarta.validation.constraints.Size(max = 255)
    private String title;

    @jakarta.validation.constraints.Pattern(regexp = "(?s).*\\S.*")
    @jakarta.validation.constraints.Size(max = 255)
    private String author;

    private String description;

    @jakarta.validation.constraints.Positive
    private Integer durationMin;

    @NotBlank
    @jakarta.validation.constraints.Size(max = 10)
    @Builder.Default
    private String language = "EN";
}
