package com.example.EV_finder_api.dto;

import com.example.EV_finder_api.entity.Role;

import java.time.LocalDateTime;

public record AdminUserResponse(
        String id, String name, String email, Role role, LocalDateTime createdAt
) {}
