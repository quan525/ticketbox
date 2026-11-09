package com.example.ticketbox.movie.dto;

import com.example.ticketbox.entity.Movies;
import com.example.ticketbox.movie.dto.request.CreateMovieRequest;
import com.example.ticketbox.movie.dto.request.UpdateMovieRequest;
import com.example.ticketbox.movie.dto.response.MovieResponse;
import org.mapstruct.*;

import java.util.List;

@Mapper(componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface MovieMapper {

    Movies toEntity(CreateMovieRequest request);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
            unmappedTargetPolicy = ReportingPolicy.IGNORE)
    void updateEntity(
            UpdateMovieRequest request,
            @MappingTarget Movies movie
    );

    MovieResponse toResponse(Movies movie);

    List<MovieResponse> toResponses(List<Movies> movies);
}

