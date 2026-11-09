package com.example.ticketbox.showseat.repository;

import com.example.ticketbox.entity.ShowSeat;
import com.example.ticketbox.enums.SeatStatus;
import com.example.ticketbox.showseat.dto.response.ShowSeatRes;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

import java.util.List;

public interface ShowSeatRepository extends JpaRepository<ShowSeat, Integer> {

    List<ShowSeat> findByShowId(Integer showId);

    List<ShowSeat> findByShowIdAndSeatIdIn(Integer showId, List<Integer> seatIds);

    List<ShowSeat> findByIdIn(List<Integer> ids);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select ss from ShowSeat ss where ss.id in :ids order by ss.id")
    List<ShowSeat> findByIdInForUpdate(@Param("ids") List<Integer> ids);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select ss from ShowSeat ss where ss.show.id = :showId order by ss.id")
    List<ShowSeat> findByShowIdForUpdate(@Param("showId") Integer showId);

    boolean existsByIdInAndStatus(
            List<Integer> ids,
            SeatStatus status
    );

    List<ShowSeatRes> getShowSeatsByShow(Integer showId);

    boolean existsByShowIdAndSeatId(Integer showId, Integer seatId);
}
