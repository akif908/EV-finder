package com.example.EV_finder_api.dto;

import com.example.EV_finder_api.entity.News;
import com.example.EV_finder_api.entity.NewsCategory;
import jakarta.validation.constraints.*;

import java.time.LocalDateTime;

public record NewsRequest(
        @NotBlank @Size(max = 150) String title,
        @NotBlank @Size(max = 300) String shortDescription,
        @NotBlank String content,
        @NotNull NewsCategory category,
        @Size(max = 120) String source,
        @Size(max = 500) String sourceUrl,
        @Size(max = 500) String imageUrl,
        @NotNull LocalDateTime publishedAt,
        @NotNull Boolean isPublished
) {
    public static NewsRequest fromEntity(News n) {
        return new NewsRequest(n.getTitle(), n.getShortDescription(), n.getContent(),
                n.getCategory(), n.getSource(), n.getSourceUrl(), n.getImageUrl(),
                n.getPublishedAt(), n.getIsPublished());
    }
}
