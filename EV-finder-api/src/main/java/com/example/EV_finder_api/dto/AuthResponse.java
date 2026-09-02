package com.example.EV_finder_api.dto;

public record AuthResponse(
        String token,
        String userId,
        String name,
        String email,
        String role
) {}
