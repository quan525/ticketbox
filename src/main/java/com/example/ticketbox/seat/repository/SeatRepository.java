package com.example.ticketbox.seat.repository;

import com.example.ticketbox.entity.Seats;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SeatRepository extends JpaRepository<Seats, Integer> {

    List<Seats> findByRoomId(Integer roomId);

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select s from Seats s where s.id in :ids order by s.id")
    List<Seats> lockByIds(@org.springframework.data.repository.query.Param("ids") List<Integer> ids);

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select s from Seats s where s.room.id = :roomId order by s.id")
    List<Seats> lockByRoom(@org.springframework.data.repository.query.Param("roomId") Integer roomId);

//    List<Seats> findByIdsIn(List<Integer> ids);

    boolean existsByRoomIdAndRowLabelAndSeatNumber(
            Integer roomId,
            String rowLabel,
            int seatNumber
    );
}
