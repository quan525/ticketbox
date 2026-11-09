package com.example.ticketbox.user.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.Length;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CreateUserRequest {
    @Length(min = 8)
    @NotBlank
    private String username;

    @Email
    @NotBlank
    private String email;

    @Length(min = 8)
    @NotBlank
    private String password;
}
