package com.example.ticketbox.booking.repository;

import com.example.ticketbox.entity.Booking;
import com.example.ticketbox.enums.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

import java.util.List;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select b from Booking b where b.id = :id")
    java.util.Optional<Booking> findByIdForUpdate(@Param("id") Long id);

    List<Booking> findByUserId(Integer userId);

    boolean existsByUserIdAndShowIdAndStatus(
            Integer userId,
            Integer showId,
            BookingStatus status
    );
}

