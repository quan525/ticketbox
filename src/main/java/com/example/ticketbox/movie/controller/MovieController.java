package com.example.ticketbox.movie.controller;

import com.example.ticketbox.entity.Movies;
import com.example.ticketbox.movie.dto.MovieMapper;
import com.example.ticketbox.movie.dto.request.CreateMovieRequest;
import com.example.ticketbox.movie.dto.request.UpdateMovieRequest;
import com.example.ticketbox.movie.dto.response.MovieResponse;
import com.example.ticketbox.movie.service.MovieService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/movies")
@RequiredArgsConstructor
public class MovieController {

    private final MovieService movieService;
    private final MovieMapper movieMapper;

    @PostMapping
    public ResponseEntity<MovieResponse> create(
            @RequestBody @Valid CreateMovieRequest req) {

        Movies movie = movieService.create(req);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(movieMapper.toResponse(movie));
    }

    @GetMapping("/{id}")
    public ResponseEntity<MovieResponse> getById(@PathVariable Integer id) {
        Movies movie = movieService.getById(id);
        return ResponseEntity.ok(movieMapper.toResponse(movie));
    }

    @GetMapping
    public ResponseEntity<List<MovieResponse>> getAll() {
        List<MovieResponse> responses =
                movieMapper.toResponses(movieService.getMovies());
        return ResponseEntity.ok(responses);
    }

    @PutMapping("/{id}")
    public ResponseEntity<MovieResponse> update(
            @PathVariable Integer id,
            @RequestBody @Valid UpdateMovieRequest req) {

        Movies movie = movieService.update(id, req);
        return ResponseEntity.ok(movieMapper.toResponse(movie));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Integer id) {
        movieService.delete(id);
    }
}
