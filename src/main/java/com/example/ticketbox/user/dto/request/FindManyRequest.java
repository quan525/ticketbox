package com.example.ticketbox.user.dto.request;

import com.example.ticketbox.enums.UserRoles;
import lombok.*;
import org.hibernate.validator.constraints.Length;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class FindManyRequest {

    @Length(min = 8)
    private String username;
    private String name;
    private UserRoles role;
}
