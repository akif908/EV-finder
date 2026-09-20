package com.example.EV_finder_api.dto;

import com.example.EV_finder_api.entity.Notification;

import java.time.LocalDateTime;

public record NotificationResponse(
        String id, String title, String message, String type,
        boolean read, LocalDateTime createdAt
) {
    public static NotificationResponse from(Notification n) {
        return new NotificationResponse(n.getId(), n.getTitle(), n.getMessage(),
                n.getType().name(), n.isRead(), n.getCreatedAt());
    }
}
