package com.example.EV_finder_api.dto;

import com.example.EV_finder_api.entity.News;
import com.example.EV_finder_api.entity.NewsCategory;

import java.time.LocalDateTime;

public record NewsResponse(
        String id,
        String title,
        String shortDescription,
        String content,
        NewsCategory category,
        String source,
        String sourceUrl,
        String imageUrl,
        LocalDateTime publishedAt,
        Boolean isPublished,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static NewsResponse from(News n) {
        return new NewsResponse(n.getId(), n.getTitle(), n.getShortDescription(), n.getContent(),
                n.getCategory(), n.getSource(), n.getSourceUrl(), n.getImageUrl(),
                n.getPublishedAt(), n.getIsPublished(), n.getCreatedAt(), n.getUpdatedAt());
    }
}
