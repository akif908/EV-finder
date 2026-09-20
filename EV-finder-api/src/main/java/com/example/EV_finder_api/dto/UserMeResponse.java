package com.example.EV_finder_api.dto;

import com.example.EV_finder_api.entity.User;

import java.time.LocalDateTime;

public record UserMeResponse(
        String id, String name, String email, String phone, String role, LocalDateTime createdAt
) {
    public static UserMeResponse from(User u) {
        return new UserMeResponse(u.getId(), u.getName(), u.getEmail(),
                u.getPhone(), u.getRole().name(), u.getCreatedAt());
    }
}
