package com.example.ticketbox.movie.repository;

import com.example.ticketbox.entity.Movies;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MovieRepository extends JpaRepository<Movies, Integer> {
}
