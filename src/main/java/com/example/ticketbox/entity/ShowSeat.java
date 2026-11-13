package com.example.ticketbox.entity;

import com.example.ticketbox.enums.SeatStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(
        name = "show_seats",
        uniqueConstraints = @UniqueConstraint(
                columnNames = {"show_id", "seat_id"}
        )
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShowSeat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "show_id", nullable = false)
    private Show show;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "seat_id", nullable = false)
    private Seats seat;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SeatStatus status;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Enumerated(EnumType.STRING)
    @Column(name = "seat_type_at_scheduling")
    private com.example.ticketbox.enums.SeatType seatTypeAtScheduling;

    public com.example.ticketbox.enums.SeatType getEffectiveSeatType() {
        return seatTypeAtScheduling != null ? seatTypeAtScheduling : seat.getSeatType();
    }

    private Instant heldUntil; // lock timeout

    @Version
    private Long version; // optimistic locking
}
