package com.example.ticketbox.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.List;

@Entity
@Table(
    name = "rooms",
    uniqueConstraints = @UniqueConstraint(columnNames = "name")
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Rooms extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, unique = true)
    private String name;

    @OneToMany(mappedBy = "room", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Seats> seats;

    @Column(nullable = false, name = "seat_rows")
    private Integer seatRows;

    private Integer capacity;
}