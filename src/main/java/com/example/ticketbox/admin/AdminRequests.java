package com.example.ticketbox.admin;

import com.example.ticketbox.enums.UserPermission;

import com.example.ticketbox.enums.SeatType;
import com.example.ticketbox.enums.UserRoles;
import jakarta.validation.constraints.*;
import java.time.LocalDateTime;

public final class AdminRequests {
    private AdminRequests() {}
    public record RoomInput(@NotBlank @Size(max = 100) String name, @Min(1) @Max(26) int rows,
                            @Min(1) @Max(30) int seatsPerRow, @NotNull SeatType seatType) {}
    public record ShowInput(@NotNull @Positive Integer movieId, @NotNull @Positive Integer roomId,
                            @NotNull @Future LocalDateTime startTime) {}
    public record ArchiveInput(boolean archived) {}
    public record PermissionsInput(boolean useRoleDefaults,
        @NotNull java.util.Set<@NotNull UserPermission> permissions) {}
    public record RoleInput(@NotNull UserRoles role) {}
}
