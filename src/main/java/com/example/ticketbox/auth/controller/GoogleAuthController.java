package com.example.ticketbox.auth.controller;

import com.example.ticketbox.auth.dto.response.AuthResponse;
import com.example.ticketbox.auth.service.GoogleAuthService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth/google")
@RequiredArgsConstructor
public class GoogleAuthController {
    private final GoogleAuthService google;
    private final com.example.ticketbox.auth.service.RefreshSessions sessions;

    public record Configuration(boolean enabled, String clientId) {}
    public record Credential(@NotBlank @Size(max = 16384) String credential) {}

    @GetMapping("/config")
    public ResponseEntity<Configuration> configuration() {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(new Configuration(!google.clientId().isBlank(), google.clientId()));
    }

    @PostMapping(consumes = "application/json")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody Credential request) {
        return sessions.start(google.login(request.credential()));
    }
}
