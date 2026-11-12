package com.example.ticketbox.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "movies")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Movies extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(nullable = false, length = 255)
    private String author;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false, name = "duration_min")
    private Integer durationMin;

    @Column(nullable = false, length = 10)
    private String language;

    @Builder.Default
    @Column(nullable = false, columnDefinition = "boolean default false")
    private boolean archived = false;
}
