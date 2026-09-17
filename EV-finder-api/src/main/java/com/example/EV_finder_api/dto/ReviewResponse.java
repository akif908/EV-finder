package com.example.EV_finder_api.dto;

import com.example.EV_finder_api.entity.StationReview;

import java.time.LocalDateTime;

public record ReviewResponse(
        String id,
        String userId,
        String userName,
        Integer rating,
        String comment,
        LocalDateTime createdAt
) {
    public static ReviewResponse from(StationReview r) {
        return new ReviewResponse(r.getId(), r.getUser().getId(), r.getUser().getName(),
                r.getRating(), r.getComment(), r.getCreatedAt());
    }
}
