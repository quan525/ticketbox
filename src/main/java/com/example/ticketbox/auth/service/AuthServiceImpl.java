package com.example.ticketbox.auth.service;

import com.example.ticketbox.auth.dto.request.LoginReq;
import com.example.ticketbox.auth.dto.request.SignupReq;
import com.example.ticketbox.auth.dto.response.AuthResponse;
import com.example.ticketbox.entity.Users;
import com.example.ticketbox.enums.UserRoles;
import com.example.ticketbox.security.JwtService;
import com.example.ticketbox.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final JwtService jwtUtil;
    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public AuthResponse signup(SignupReq req) {
        if (userRepository.findByUsername(req.getUsername()).isPresent()) {
            throw new IllegalStateException("This username is already taken");
        }

        String encodedPassword = passwordEncoder.encode(req.getPassword());

        Users user = Users.builder()
                .username(req.getUsername())
                .password(encodedPassword)
                .role(UserRoles.USER)
                .build();

        userRepository.save(user);

        String token = jwtUtil.generateToken(user.getUsername());
        return new AuthResponse(token);
    }

    @Override
    public AuthResponse login(LoginReq req) {

        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        req.getUsername(),
                        req.getPassword()
                )
        );

        String token = jwtUtil.generateToken(req.getUsername());
        return new AuthResponse(token);
    }
}
