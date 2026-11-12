package com.example.ticketbox.movie.repository;

import com.example.ticketbox.entity.Movies;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MovieRepository extends JpaRepository<Movies, Integer> {
    java.util.List<Movies> findByArchivedFalse();

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select m from Movies m where m.id = :id")
    java.util.Optional<Movies> findByIdForUpdate(@org.springframework.data.repository.query.Param("id") Integer id);
}
