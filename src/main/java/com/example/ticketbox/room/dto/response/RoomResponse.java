package com.example.ticketbox.room.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoomResponse {
    Integer id;
    String name;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
}