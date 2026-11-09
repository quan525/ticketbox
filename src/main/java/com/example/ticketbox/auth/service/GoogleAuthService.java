package com.example.ticketbox.auth.service;

import com.example.ticketbox.auth.dto.response.AuthResponse;
import com.example.ticketbox.entity.Users;
import com.example.ticketbox.enums.UserRoles;
import com.example.ticketbox.security.JwtService;
import com.example.ticketbox.user.repository.UserRepository;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.List;
import java.util.UUID;

@Service
public class GoogleAuthService {
    private final String clientId;
    private final GoogleIdTokenVerifier verifier;
    private final UserRepository users;
    private final PasswordEncoder passwords;
    private final JwtService jwt;

    @Autowired
    public GoogleAuthService(@Value("${auth.google.client-id:}") String clientId,
                             UserRepository users, PasswordEncoder passwords, JwtService jwt) {
        this(clientId, new GoogleIdTokenVerifier.Builder(new NetHttpTransport(), GsonFactory.getDefaultInstance())
                .setAudience(List.of(clientId.trim())).build(), users, passwords, jwt);
    }

    GoogleAuthService(String clientId, GoogleIdTokenVerifier verifier, UserRepository users, PasswordEncoder passwords, JwtService jwt) {
        this.clientId = clientId.trim();
        this.verifier = verifier;
        this.users = users;
        this.passwords = passwords;
        this.jwt = jwt;
    }

    public String clientId() { return clientId; }

    public AuthResponse login(String credential) {
        if (clientId.isBlank()) throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Google sign-in is not configured");
        GoogleIdToken token;
        try {
            // Google's library checks the signature, audience, issuer and expiration,
            // and caches Google's rotating public keys. Never trust decoded claims alone.
            token = verifier.verify(credential);
        } catch (GeneralSecurityException | IOException | IllegalArgumentException exception) {
            throw new BadCredentialsException("Google sign-in could not be verified");
        }
        if (token == null || token.getPayload().getSubject() == null || token.getPayload().getSubject().isBlank()
                || !Boolean.TRUE.equals(token.getPayload().getEmailVerified())
                || token.getPayload().getEmail() == null || token.getPayload().getEmail().isBlank()) {
            throw new BadCredentialsException("Google sign-in could not be verified");
        }
        var identity = token.getPayload();
        // Email matching never links or grants access to an existing password account.
        var user = users.findByGoogleSubject(identity.getSubject()).orElseGet(() -> createUser(identity));
        return new AuthResponse(jwt.generateToken(user.getUsername()));
    }

    private Users createUser(GoogleIdToken.Payload identity) {
        var user = Users.builder()
                .googleSubject(identity.getSubject())
                .username("google_" + UUID.randomUUID().toString().replace("-", ""))
                .email(identity.getEmail())
                .firstName(profileName(identity.get("given_name")))
                .lastName(profileName(identity.get("family_name")))
                .password(passwords.encode(UUID.randomUUID().toString()))
                .role(UserRoles.USER).build();
        try {
            return users.saveAndFlush(user);
        } catch (DataIntegrityViolationException exception) {
            // Repository transactions are separate so a simultaneous first sign-in
            // can read the winner after the unique-subject constraint rejects this insert.
            return users.findByGoogleSubject(identity.getSubject()).orElseThrow(() -> exception);
        }
    }

    private static String profileName(Object value) {
        if (!(value instanceof String name)) return null;
        return name.substring(0, Math.min(50, name.length()));
    }
}
