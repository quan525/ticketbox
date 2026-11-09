package com.example.ticketbox.movie.service;

import com.example.ticketbox.entity.Movies;
import com.example.ticketbox.movie.dto.request.CreateMovieRequest;
import com.example.ticketbox.movie.dto.request.UpdateMovieRequest;

import java.util.List;

public interface MovieService {

    Movies create(CreateMovieRequest req);

    Movies getById(Integer id);

    List<Movies> getMovies();

    Movies update(Integer id, UpdateMovieRequest req);

    void delete(Integer id);
}
