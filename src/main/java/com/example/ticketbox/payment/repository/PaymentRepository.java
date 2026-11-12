package com.example.ticketbox.payment.repository;

import com.example.ticketbox.entity.Payments;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

import java.util.Collection;
import java.util.List;

public interface PaymentRepository extends JpaRepository<Payments, Long>{

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Payments p where p.id = :id")
    java.util.Optional<Payments> findByIdForUpdate(@Param("id") Long id);

    @Query("select p.booking.id from Payments p where p.id = :id")
    java.util.Optional<Long> findBookingId(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Payments p where p.booking.id = :bookingId order by p.id")
    List<Payments> findByBookingIdForUpdate(@Param("bookingId") Long bookingId);

    List<Payments> findByBookingIdOrderByCreatedAtDesc(Long bookingId);

    boolean existsByBookingIdAndStatusIn(Long bookingId, Collection<com.example.ticketbox.enums.PaymentStatus> statuses);

    java.util.Optional<Payments> findBySepayReference(String reference);

    boolean existsByTransactionId(String transactionId);
}
