package com.example.EV_finder_api.dto;

import com.example.EV_finder_api.entity.NewsArticle;

import java.time.LocalDateTime;

/** One energy / fuel / EV headline for the app's news section. */
public record NewsResponse(
        String id,
        String title,
        String link,
        String source,
        String category,
        String summary,
        LocalDateTime publishedAt
) {
    public static NewsResponse from(NewsArticle a) {
        return new NewsResponse(a.getId(), a.getTitle(), a.getLink(), a.getSource(),
                a.getCategory(), a.getSummary(), a.getPublishedAt());
    }
}
