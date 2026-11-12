package com.example.ticketbox.show.repository;

import com.example.ticketbox.entity.Show;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public interface ShowRepository extends JpaRepository<Show, Integer>, JpaSpecificationExecutor<Show> {
    boolean existsByRoomIdAndIdNotAndStartTimeLessThanAndEndTimeGreaterThan(Integer roomId, Integer id, LocalDateTime end, LocalDateTime start);
    boolean existsByMovieIdAndEndTimeAfter(Integer movieId, LocalDateTime now);

    default List<Show> findAllByFilter(String movieTitle, LocalDateTime startTime) {
        Specification<Show> filters = (show, query, builder) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (movieTitle != null) {
                predicates.add(builder.like(
                        builder.lower(show.join("movie").get("title")),
                        builder.lower(builder.literal("%" + movieTitle + "%"))));
            }
            if (startTime != null) {
                predicates.add(builder.greaterThanOrEqualTo(show.get("startTime"), startTime));
            }
            return builder.and(predicates.toArray(Predicate[]::new));
        };
        return findAll(filters, Sort.by("startTime"));
    }
}
