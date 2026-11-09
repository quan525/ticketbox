package com.example.ticketbox.auth.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginReq {
    @NotNull
    private String username;

    @NotNull
    private String password;
}