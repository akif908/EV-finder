package com.example.EV_finder_api.dto;

import com.example.EV_finder_api.entity.Review;

import java.time.LocalDateTime;
import java.util.List;

public record StationReviewsResponse(
        double averageRating,
        int count,
        List<Item> reviews
) {
    public record Item(String id, String userName, int rating, String comment, LocalDateTime createdAt) {
        public static Item from(Review r) {
            return new Item(r.getId(), r.getUser().getName(), r.getRating(),
                    r.getComment(), r.getCreatedAt());
        }
    }
}
