package com.example.ticketbox.seat.repository;

import com.example.ticketbox.entity.Seats;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SeatRepository extends JpaRepository<Seats, Integer> {

    List<Seats> findByRoomId(Integer roomId);

//    List<Seats> findByIdsIn(List<Integer> ids);

    boolean existsByRoomIdAndRowLabelAndSeatNumber(
            Integer roomId,
            String rowLabel,
            int seatNumber
    );
}
