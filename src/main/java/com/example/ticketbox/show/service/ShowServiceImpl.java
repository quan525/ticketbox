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
    private final com.example.ticketbox.room.repository.RoomRepository roomRepository;
    private final com.example.ticketbox.movie.repository.MovieRepository movieRepository;
    private final jakarta.persistence.EntityManager entityManager;

    @Transactional
    public Show createShow(CreateShowReq req) {

        Movies movie = movieRepository.findByIdForUpdate(req.getMovieId()).orElseThrow(() -> new EntityNotFoundException("Movie not found"));
        entityManager.refresh(movie);
        if (movie.isArchived()) throw new IllegalStateException("Restore the movie before scheduling a screening");

        Rooms room = roomRepository.findByIdForUpdate(req.getRoomId()).orElseThrow(() -> new EntityNotFoundException("Room not found"));

        Show show = showMapper.toEntity(req);
        show.setMovie(movie);
        show.setRoom(room);
        validateTimes(show.getStartTime(), show.getEndTime());
        validateRoomAvailability(show);

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
        roomRepository.findByIdForUpdate(show.getRoom().getId()).orElseThrow(() -> new EntityNotFoundException("Room not found"));
        validateRoomAvailability(show);

        return showRepo.save(show);
    }

    private void validateTimes(java.time.LocalDateTime startTime, java.time.LocalDateTime endTime) {
        if (startTime != null && endTime != null && !endTime.isAfter(startTime)) {
            throw new IllegalArgumentException("Show end time must be after start time");
        }
    }

    private void validateRoomAvailability(Show show) {
        if (show.getStartTime() != null && show.getEndTime() != null &&
                showRepo.existsByRoomIdAndIdNotAndStartTimeLessThanAndEndTimeGreaterThan(
                        show.getRoom().getId(), show.getId() == null ? -1 : show.getId(), show.getEndTime(), show.getStartTime())) {
            throw new IllegalStateException("This room already has a screening during that time");
        }
    }

}

