package com.example.ticketbox.user.dto.request;

import com.example.ticketbox.enums.UserRoles;

public record UpdateUserRoleRequet(
        UserRoles role
) {
}
