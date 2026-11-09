package com.example.ticketbox.auth.service;

import com.example.ticketbox.auth.dto.request.LoginReq;
import com.example.ticketbox.auth.dto.request.SignupReq;
import com.example.ticketbox.auth.dto.response.AuthResponse;

public interface AuthService {
    AuthResponse signup(SignupReq req);

    AuthResponse login(LoginReq req);
}