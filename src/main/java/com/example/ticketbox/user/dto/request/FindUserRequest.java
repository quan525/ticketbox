package com.example.ticketbox.user.dto.request;

import com.example.ticketbox.enums.UserRoles;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class FindUserRequest {
    String username;

    String name;

    UserRoles role;
}
