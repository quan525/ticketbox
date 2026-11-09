package com.example.ticketbox.auth.service;

import com.example.ticketbox.entity.Users;
import com.example.ticketbox.enums.UserRoles;
import com.example.ticketbox.security.JwtService;
import com.example.ticketbox.user.repository.UserRepository;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;
import java.util.Optional;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class GoogleAuthServiceTest {
    GoogleIdTokenVerifier verifier = mock(GoogleIdTokenVerifier.class);
    UserRepository users = mock(UserRepository.class);
    PasswordEncoder passwords = mock(PasswordEncoder.class);
    JwtService jwt = mock(JwtService.class);
    GoogleAuthService auth = new GoogleAuthService("web-client", verifier, users, passwords, jwt);
    GoogleIdToken.Payload payload;

    @BeforeEach void setup() throws Exception {
        payload = new GoogleIdToken.Payload().setSubject("google-subject").setEmail("person@gmail.com").setEmailVerified(true);
        payload.set("given_name", "Alice");
        var token = mock(GoogleIdToken.class);
        when(token.getPayload()).thenReturn(payload);
        when(verifier.verify("credential")).thenReturn(token);
        when(users.findByGoogleSubject(anyString())).thenReturn(Optional.empty());
        when(passwords.encode(anyString())).thenReturn("encoded-random-password");
        when(users.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(jwt.generateToken(anyString())).thenReturn("ticketbox-token");
    }
    @Test void createsRegularAccountFromVerifiedSubject() {
        assertThat(auth.login("credential").getToken()).isEqualTo("ticketbox-token");
        var captor = org.mockito.ArgumentCaptor.forClass(Users.class);
        verify(users).saveAndFlush(captor.capture());
        assertThat(captor.getValue().getGoogleSubject()).isEqualTo("google-subject");
        assertThat(captor.getValue().getRole()).isEqualTo(UserRoles.USER);
        assertThat(captor.getValue().getPassword()).isEqualTo("encoded-random-password");
        verify(users, never()).findByEmail(anyString());
    }
    @Test void returningSubjectKeepsItsExistingAccount() {
        when(users.findByGoogleSubject("google-subject")).thenReturn(Optional.of(Users.builder().username("existing-user").build()));
        auth.login("credential");
        verify(jwt).generateToken("existing-user");
        verify(users, never()).saveAndFlush(any());
    }
    @Test void invalidTokenCannotCreateAnAccount() throws Exception {
        when(verifier.verify("credential")).thenReturn(null);
        assertThatThrownBy(() -> auth.login("credential")).isInstanceOf(BadCredentialsException.class);
        verifyNoInteractions(users, jwt);
    }
    @Test void unverifiedEmailIsRejected() {
        payload.setEmailVerified(false);
        assertThatThrownBy(() -> auth.login("credential")).isInstanceOf(BadCredentialsException.class);
        verifyNoInteractions(users, jwt);
    }
    @Test void missingSubjectIsRejected() {
        payload.setSubject("");
        assertThatThrownBy(() -> auth.login("credential")).isInstanceOf(BadCredentialsException.class);
    }
    @Test void verificationFailureIsHandledWithoutIssuingToken() throws Exception {
        when(verifier.verify("credential")).thenThrow(new java.security.GeneralSecurityException("invalid signature"));
        assertThatThrownBy(() -> auth.login("credential")).isInstanceOf(BadCredentialsException.class);
        verifyNoInteractions(users, jwt);
    }
    @Test void disabledConfigurationDoesNotAttemptVerification() {
        var disabled = new GoogleAuthService("", verifier, users, passwords, jwt);
        assertThatThrownBy(() -> disabled.login("credential")).isInstanceOf(ResponseStatusException.class);
        verifyNoInteractions(verifier, users, jwt);
    }
}
