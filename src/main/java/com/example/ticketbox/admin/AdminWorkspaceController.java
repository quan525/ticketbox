package com.example.ticketbox.admin;

import com.example.ticketbox.movie.dto.response.MovieResponse;
import com.example.ticketbox.show.dto.Response.ShowResponse;
import com.example.ticketbox.user.dto.response.UserDetailResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import java.security.Principal;

@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminWorkspaceController {
    private final AdminWorkspaceService admin;
    @GetMapping("/workspace")
    public ResponseEntity<AdminWorkspaceService.Workspace> workspace(Principal actor) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(admin.workspace(actor.getName()));
    }
    @PostMapping("/rooms")
    @ResponseStatus(HttpStatus.CREATED)
    public AdminWorkspaceService.RoomInfo createRoom(@Valid @RequestBody AdminRequests.RoomInput input) { return admin.createRoom(input); }
    @PostMapping("/shows")
    @ResponseStatus(HttpStatus.CREATED)
    public ShowResponse schedule(@Valid @RequestBody AdminRequests.ShowInput input) { return admin.schedule(input); }
    @PatchMapping("/movies/{id}/archive")
    public MovieResponse archive(@PathVariable Integer id, @RequestBody AdminRequests.ArchiveInput input) { return admin.archive(id, input.archived()); }
    @PatchMapping("/users/{id}/permissions")
    public UserDetailResponse permissions(@PathVariable Integer id, @Valid @RequestBody AdminRequests.PermissionsInput input, Principal actor) {
        return admin.changePermissions(id, input, actor.getName());
    }
    @PatchMapping("/users/{id}/role")
    public UserDetailResponse role(@PathVariable Integer id, @Valid @RequestBody AdminRequests.RoleInput input, Principal actor) { return admin.changeRole(id, input.role(), actor.getName()); }
}
