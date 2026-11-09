package com.example.ticketbox.show.repository;

import com.example.ticketbox.entity.Show;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface ShowRepository extends JpaRepository<Show, Integer> {
    @Query("""
                SELECT s FROM Show s
                JOIN s.movie m
                WHERE (:movieTitle IS NULL OR LOWER(m.title) LIKE LOWER(CONCAT('%', :movieTitle, '%')))
                  AND (:startTime IS NULL OR s.startTime >= :startTime)
                ORDER BY s.startTime ASC
            """)
    List<Show> findAllByFilter(
            @Param("movieTitle") String movieTitle,
            @Param("startTime") LocalDateTime startTime
    );

}


//WHERE (:movieTitle IS NULL OR m.title = :movieTitle)
//AND (:startTime IS NULL OR s.startTime > :startTime)

//WHERE(COALESCE(:movieTitle, '') IS NULL OR m.title = :movieTitle)
//AND (COALESCE(:startTime, '') IS NULL OR s.startTime > :startTime)
