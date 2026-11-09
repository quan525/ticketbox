package com.example.ticketbox.movie.service;

import com.example.ticketbox.entity.Movies;
import com.example.ticketbox.movie.dto.MovieMapper;
import com.example.ticketbox.movie.dto.request.CreateMovieRequest;
import com.example.ticketbox.movie.dto.request.UpdateMovieRequest;
import com.example.ticketbox.movie.repository.MovieRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MovieServiceImpl implements MovieService {

    private final MovieRepository movieRepository;
    private final MovieMapper movieMapper;

    @Override
    public Movies create(CreateMovieRequest req) {
        Movies movie = movieMapper.toEntity(req);
        return movieRepository.save(movie);
    }

    @Override
    public Movies getById(Integer id) {
        return findMovieOrThrow(id);
    }

    @Override
    public List<Movies> getMovies() {
        return movieRepository.findAll();
    }

    @Override
    public Movies update(Integer id, UpdateMovieRequest req) {
        Movies movie = findMovieOrThrow(id);
        movieMapper.updateEntity(req, movie);
        return movieRepository.save(movie);
    }

    @Override
    public void delete(Integer id) {
        movieRepository.delete(findMovieOrThrow(id));
    }

    private Movies findMovieOrThrow(Integer id) {
        return movieRepository.findById(id)
                .orElseThrow(() ->
                        new EntityNotFoundException("Movie with id " + id + " not found"));
    }
}
