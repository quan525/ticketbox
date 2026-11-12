package com.example.ticketbox.auth.controller;

import com.example.ticketbox.auth.dto.request.LoginReq;
import com.example.ticketbox.auth.dto.request.SignupReq;
import com.example.ticketbox.auth.dto.response.AuthResponse;
import com.example.ticketbox.auth.service.AuthService;
import com.example.ticketbox.security.JwtService;
import com.example.ticketbox.security.UserDetailsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final com.example.ticketbox.auth.service.RefreshSessions sessions;
    private final com.example.ticketbox.user.repository.UserRepository userRepository;

    @GetMapping("/me")
    public com.example.ticketbox.user.dto.response.UserDetailResponse me(org.springframework.security.core.Authentication authentication) {
        return com.example.ticketbox.user.dto.response.UserDetailResponse.from(userRepository.findByUsername(authentication.getName()).orElseThrow());
    }

    @Autowired
    public JwtService jwtService;

    @Autowired
    public UserDetailsService userDetailsService;


    @PostMapping("/signup")
    public ResponseEntity<AuthResponse> signup(@RequestBody @Valid SignupReq req) {
        return sessions.start(authService.signup(req));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody @Valid LoginReq req) {
        return sessions.start(authService.login(req));
    }

    public record SessionRequest(
            @jakarta.validation.constraints.NotBlank @jakarta.validation.constraints.Size(max = 255) String username) {}

    @PostMapping(value = "/refresh", consumes = "application/json")
    public ResponseEntity<AuthResponse> refresh(
            @CookieValue(name = com.example.ticketbox.auth.service.RefreshSessions.COOKIE, required = false) String token,
            @Valid @RequestBody SessionRequest request) {
        return sessions.refresh(token, request.username());
    }

    @PostMapping(value = "/logout", consumes = "application/json")
    public ResponseEntity<Void> logout(
            @CookieValue(name = com.example.ticketbox.auth.service.RefreshSessions.COOKIE, required = false) String token,
            @Valid @RequestBody SessionRequest request) {
        return sessions.logout(token, request.username());
    }

    @PostMapping("/validation")
    public ResponseEntity<Boolean> validateToken(
            @RequestHeader("Authorization") String token) {

        token = token.substring(7);

        String username = jwtService.extractUsername(token);

        UserDetails user = userDetailsService.loadUserByUsername(username);

        boolean valid = jwtService.validateToken(token, user);

        return ResponseEntity.ok(valid);
    }
}
