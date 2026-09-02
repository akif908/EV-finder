package com.example.EV_finder_api.service;

import com.example.EV_finder_api.dto.AuthResponse;
import com.example.EV_finder_api.dto.LoginRequest;
import com.example.EV_finder_api.dto.RegisterRequest;

/** Authentication business logic (register + login). */
public interface AuthService {

    AuthResponse register(RegisterRequest request);

    AuthResponse login(LoginRequest request);
}
