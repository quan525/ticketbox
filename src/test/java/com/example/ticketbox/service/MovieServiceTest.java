package com.example.ticketbox.service;

import com.example.ticketbox.TestDataUtils;
import com.example.ticketbox.entity.Movies;
import com.example.ticketbox.movie.dto.request.CreateMovieRequest;
import com.example.ticketbox.movie.dto.request.UpdateMovieRequest;
import com.example.ticketbox.movie.service.MovieService;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@org.springframework.transaction.annotation.Transactional
class MovieServiceTest {
    @Autowired
    MovieService movieService;

    @Test
    void CreateMovieTest() {
        CreateMovieRequest movieRequestA = TestDataUtils.CreateMovieRequestA();

        Movies movie = movieService.create(movieRequestA);
        assertThat(movie.getTitle()).isEqualTo("Onyx light");
    }

    @Test
    void findMovieTest() {
        CreateMovieRequest movieRequestA = TestDataUtils.CreateMovieRequestA();

        Movies movie = movieService.create(movieRequestA);
        Movies movieResponse = movieService.getById(movie.getId());
        assertThat(movieResponse.getId()).isEqualTo(movie.getId());

    }

    @Test
    void findMovieFailTest() {
        CreateMovieRequest movieRequestA = TestDataUtils.CreateMovieRequestA();

        Movies movie = movieService.create(movieRequestA);

        assertThatThrownBy(() -> movieService.getById(Integer.MAX_VALUE))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Movie with id 2147483647 not found");
    }

    @Test
    void UpdateMovieTest() {
        Movies movieResA = movieService.create(TestDataUtils.CreateMovieRequestA());

        UpdateMovieRequest updateReq = UpdateMovieRequest.builder()
                .title("Dopamine")
                .author("Chritian")
                .durationMin(23)
                .build();

        Movies updatedMovie = movieService.update(movieResA.getId(), updateReq);
        assertThat(updatedMovie.getTitle()).isEqualTo(updateReq.getTitle());
        assertThat(updatedMovie.getAuthor()).isEqualTo(updateReq.getAuthor());
        assertThat(updatedMovie.getDurationMin()).isEqualTo(updateReq.getDurationMin());
    }

}