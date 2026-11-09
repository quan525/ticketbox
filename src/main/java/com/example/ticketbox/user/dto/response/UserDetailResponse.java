package com.example.ticketbox.user.dto.response;

import com.example.ticketbox.entity.Users;
import com.example.ticketbox.enums.UserRoles;

import java.util.Objects;

public record UserDetailResponse(
        Integer id,
        String username,
        String email,
        String firstName,
        String lastName,
        UserRoles role
) {
    public static UserDetailResponse from(Users user) {
        Objects.requireNonNull(user, "User must not be null");

        return new UserDetailResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getRole()
        );
    }
}
