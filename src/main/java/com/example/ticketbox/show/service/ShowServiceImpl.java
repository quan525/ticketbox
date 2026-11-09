package com.example.ticketbox.show.service;

import com.example.ticketbox.entity.Movies;
import com.example.ticketbox.entity.Rooms;
import com.example.ticketbox.entity.Show;
import com.example.ticketbox.movie.service.MovieService;
import com.example.ticketbox.room.service.RoomService;
import com.example.ticketbox.show.dto.Request.CreateShowReq;
import com.example.ticketbox.show.dto.Request.GetShowsReq;
import com.example.ticketbox.show.dto.Request.UpdateShowReq;
import com.example.ticketbox.show.dto.ShowMapper;
import com.example.ticketbox.show.repository.ShowRepository;
import com.example.ticketbox.showseat.repository.ShowSeatRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class ShowServiceImpl implements ShowService {

    private final ShowRepository showRepo;
    private final MovieService movieService;
    private final RoomService roomService;
    private final ShowMapper showMapper;
    private final ShowSeatRepository showSeatRepository;

    @Transactional
    public Show createShow(CreateShowReq req) {

        Movies movie = movieService.getById(req.getMovieId());

        Rooms room = roomService.getById(req.getRoomId());

        Show show = showMapper.toEntity(req);
        show.setMovie(movie);
        show.setRoom(room);
        validateTimes(show.getStartTime(), show.getEndTime());

        // 1️⃣ SAVE SHOW FIRST (ID REQUIRED)
        Show savedShow = showRepo.save(show);

        // 2️⃣ AUTO-GENERATE SHOW SEATS

        return savedShow;
    }

    @Transactional(readOnly = true)
    public Show getShowById(Integer id) {
        return showRepo.findById(id)
                .orElseThrow(() ->
                        new EntityNotFoundException("Show not found")
                );
    }

    @Transactional(readOnly = true)
    public List<Show> getShows(GetShowsReq req) {
        return showRepo.findAllByFilter(req.getMovieTitle(), req.getStartTime());
    }

    @Override
    public Show updateShow(Integer showId, UpdateShowReq req) {
        Show show = showRepo.findById(showId)
                .orElseThrow(() ->
                        new EntityNotFoundException("Show not found with id " + showId)
                );

        if (req.getMovieId() != null) {
            show.setMovie(movieService.getById(req.getMovieId()));
        }
        if (req.getRoomId() != null && !req.getRoomId().equals(show.getRoom().getId())) {
            if (!showSeatRepository.findByShowId(showId).isEmpty()) {
                throw new IllegalStateException("Cannot change room after show seats are generated");
            }
            show.setRoom(roomService.getById(req.getRoomId()));
        }

        showMapper.update(req, show);
        validateTimes(show.getStartTime(), show.getEndTime());

        return showRepo.save(show);
    }

    private void validateTimes(java.time.LocalDateTime startTime, java.time.LocalDateTime endTime) {
        if (startTime != null && endTime != null && !endTime.isAfter(startTime)) {
            throw new IllegalArgumentException("Show end time must be after start time");
        }
    }

}

