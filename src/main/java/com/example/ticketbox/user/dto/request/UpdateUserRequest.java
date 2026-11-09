package com.example.ticketbox.user.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UpdateUserRequest {
    String firstName;
    String lastName;
    LocalDate dob;
}
