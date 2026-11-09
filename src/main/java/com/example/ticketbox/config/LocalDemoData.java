package com.example.ticketbox.config;

import com.example.ticketbox.entity.*;
import com.example.ticketbox.enums.*;
import com.example.ticketbox.movie.repository.MovieRepository;
import com.example.ticketbox.room.repository.RoomRepository;
import com.example.ticketbox.seat.repository.SeatRepository;
import com.example.ticketbox.show.repository.ShowRepository;
import com.example.ticketbox.showseat.repository.ShowSeatRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;

@Component
@Profile("local")
@RequiredArgsConstructor
public class LocalDemoData implements CommandLineRunner {
    private final MovieRepository movies;
    private final RoomRepository rooms;
    private final SeatRepository seats;
    private final ShowRepository shows;
    private final ShowSeatRepository showSeats;

    @Override
    @Transactional
    public void run(String... args) {
        if (movies.count() > 0) return;
        var titles = new String[]{"The Last Orbit", "Midnight in Kyoto", "Wild Current", "A Little More Time", "Concrete Dreams", "Beyond the Pines", "The Quiet Season", "After the Applause"};
        var directors = new String[]{"Alex Morgan", "Yuki Tanaka", "Sofia Reed", "Daniel Park", "Maya Ellis", "Oliver West", "Lena Bloom", "Noah Chen"};
        var descriptions = new String[]{"One final mission. A universe of possibilities. An astronaut follows a mysterious signal beyond the edge of the known world.", "Two strangers cross paths on the last train through Kyoto, discovering a city and a connection neither expected.", "A marine biologist returns to her coastal hometown to protect the ocean that shaped her childhood.", "An unexpected friendship reminds two neighbors that it is never too late to begin again.", "A young architect finds beauty, belonging, and a new perspective in a city that never stands still.", "Two siblings retrace a childhood journey through the mountains and discover what it means to come home.", "A quiet village prepares for a summer that will change everything.", "Behind the curtain, a musician learns to find her own voice."};
        var room = rooms.save(Rooms.builder().name("Screen 01").seatRows(6).capacity(60).build());
        var roomSeats = new ArrayList<Seats>();
        for (int row = 0; row < 6; row++) for (int number = 1; number <= 10; number++) {
            roomSeats.add(seats.save(Seats.builder().room(room).rowLabel(String.valueOf((char)('A' + row))).seatNumber(number).seatType(row >= 3 ? SeatType.VIP : SeatType.STANDARD).build()));
        }
        // Distinct days avoid overlapping screenings in the same room.
        for (int i = 0; i < titles.length; i++) {
            var movie = movies.save(Movies.builder().title(titles[i]).author(directors[i]).durationMin(110 + i * 3).language(i == 1 ? "JA" : "EN").description(descriptions[i]).build());
            if (i >= 6) continue;
            for (int day = 0; day < 2; day++) {
                var start = LocalDate.now().plusDays(1 + i + day * 6).atTime(18, 30);
                var show = shows.save(Show.builder().movie(movie).room(room).startTime(start).endTime(start.plusMinutes(movie.getDurationMin())).build());
                for (var seat : roomSeats) showSeats.save(ShowSeat.builder().show(show).seat(seat).status(SeatStatus.AVAILABLE).price(new BigDecimal(seat.getSeatType() == SeatType.VIP ? "150000" : "100000")).build());
            }
        }
    }
}
