package com.example.ticketbox.service;

import com.example.ticketbox.auth.dto.request.LoginReq;
import com.example.ticketbox.auth.dto.request.SignupReq;
import com.example.ticketbox.auth.dto.response.AuthResponse;
import com.example.ticketbox.auth.service.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
@ActiveProfiles("test")
@org.springframework.transaction.annotation.Transactional
public class AuthServiceTest {
    @Autowired
    private AuthService authService;
    @Autowired private com.example.ticketbox.security.JwtService jwt;
    @Autowired private com.example.ticketbox.user.repository.UserRepository users;

    @Test
    void SignInTest() {
        SignupReq signupReq = SignupReq.builder().username("newuser").password("password").build();
        AuthResponse authResponse = authService.signup(signupReq);

        System.out.println(authResponse);
        assertNotNull(authResponse);
        org.junit.jupiter.api.Assertions.assertEquals("newuser", jwt.extractUsername(authResponse.getToken()));
        org.junit.jupiter.api.Assertions.assertNotEquals("password", users.findByUsername("newuser").orElseThrow().getPassword());
    }

    @Test
    void LoginTest() {
        authService.signup(SignupReq.builder().username("admin").password("password123").build());
        LoginReq loginReq = LoginReq.builder().username("admin").password("password123").build();

        AuthResponse authResponse = authService.login(loginReq);

        assertNotNull(authResponse);
        org.junit.jupiter.api.Assertions.assertEquals("admin", jwt.extractUsername(authResponse.getToken()));
    }

    @Test void duplicateSignupIsRejected() {
        var req = SignupReq.builder().username("duplicate").password("password123").build();
        authService.signup(req);
        org.junit.jupiter.api.Assertions.assertThrows(IllegalStateException.class, () -> authService.signup(req));
    }
}
