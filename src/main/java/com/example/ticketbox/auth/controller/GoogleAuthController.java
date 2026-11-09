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

    public record Configuration(boolean enabled, String clientId) {}
    public record Credential(@NotBlank @Size(max = 16384) String credential) {}

    @GetMapping("/config")
    public ResponseEntity<Configuration> configuration() {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(new Configuration(!google.clientId().isBlank(), google.clientId()));
    }

    // Only same-origin JSON requests from the GIS JavaScript callback are accepted.
    // This endpoint issues a bearer token; it does not set an authentication cookie.
    @PostMapping(consumes = "application/json")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody Credential request) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(google.login(request.credential()));
    }
}
