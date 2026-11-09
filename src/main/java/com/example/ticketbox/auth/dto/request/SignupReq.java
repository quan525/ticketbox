package com.example.ticketbox.auth.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.hibernate.validator.constraints.Length;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
//@OneOfNotNull
public class SignupReq {
    @NotBlank
    @Size(min = 3, max = 30, message = "Username must be 3–30 characters")
    private String username;

    @NotBlank(message = "Password is required")
    @Length(min = 8, max = 64, message = "Password must be 8–64 characters")
    private String password;

}
