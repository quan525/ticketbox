package com.example.ticketbox.service;

import com.example.ticketbox.TestDataFactory;
import com.example.ticketbox.entity.Movies;
import com.example.ticketbox.entity.Rooms;
import com.example.ticketbox.entity.Show;
import com.example.ticketbox.room.dto.request.CreateRoomRequest;
import com.example.ticketbox.room.service.RoomService;
import com.example.ticketbox.show.dto.Request.CreateShowReq;
import com.example.ticketbox.show.dto.Request.GetShowsReq;
import com.example.ticketbox.show.dto.Request.UpdateShowReq;
import com.example.ticketbox.show.service.ShowServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;


@SpringBootTest
@org.springframework.transaction.annotation.Transactional
class ShowServiceTest {
    @Autowired
    TestDataFactory dataFactory;
    @Autowired
    ShowServiceImpl showService;

    @Autowired
    RoomService roomService;

    @Test
    void createShowTest() {
        Rooms room = dataFactory.createRoom();
        dataFactory.createSeatsForRoom(room); // ✅ IMPORTANT

        Movies movie = dataFactory.createMovie();

        CreateShowReq req = CreateShowReq.builder()
                .roomId(room.getId())
                .movieId(movie.getId())
                .build();

        Show res = showService.createShow(req);

        assertNotNull(res);
    }


    @Test
    void getShowTest() {
        Rooms room = dataFactory.createRoom();
        dataFactory.createSeatsForRoom(room); // ✅

        Movies movie = dataFactory.createMovie();

        showService.createShow(
                CreateShowReq.builder()
                        .roomId(room.getId())
                        .movieId(movie.getId())
                        .build()
        );

        List<Show> shows = showService.getShows(GetShowsReq.builder().build());

        assertEquals(1, shows.size());
    }

    @Test
    void updateShowTest() {
        CreateRoomRequest roomDReq = CreateRoomRequest.builder().name("roomD").seatRows(22).build();
        Rooms room = roomService.create(roomDReq);
        dataFactory.createSeatsForRoom(room); // ✅

        Movies movie = dataFactory.createMovie();

        Show show = showService.createShow(
                CreateShowReq.builder()
                        .roomId(room.getId())
                        .movieId(movie.getId())
                        .build()
        );

        UpdateShowReq showReq = UpdateShowReq.builder()
                .startTime(LocalDateTime.of(2025, 1, 10, 10, 0))
                .endTime(LocalDateTime.of(2025, 1, 10, 12, 0))
                .build();

        Show updatedShow = showService.updateShow(show.getId(), showReq);

        assertEquals(showReq.getStartTime(), updatedShow.getStartTime());
        assertEquals(showReq.getEndTime(), updatedShow.getEndTime());
    }

    @Test
    void createShow_rejectsInvalidTimeRange() {
        Rooms room = dataFactory.createRoom();
        Movies movie = dataFactory.createMovie();

        CreateShowReq request = CreateShowReq.builder()
                .roomId(room.getId())
                .movieId(movie.getId())
                .startTime(LocalDateTime.of(2026, 1, 10, 12, 0))
                .endTime(LocalDateTime.of(2026, 1, 10, 10, 0))
                .build();

        assertThrows(IllegalArgumentException.class,
                () -> showService.createShow(request));
    }

}
