package com.example.EV_finder_api.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * A Bangladesh-focused energy & fuel news article. Nation-wide by design —
 * no location filtering, every user sees the same published feed.
 */
@Entity
@Table(name = "news", indexes = {
        @Index(name = "idx_news_published", columnList = "is_published, published_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class News {

    @Id
    @Column(name = "news_id", length = 36)
    private String id;

    @Column(nullable = false, length = 150)
    private String title;

    /** One/two-line summary shown on the home & list cards. */
    @Column(name = "short_description", nullable = false, length = 300)
    private String shortDescription;

    /** Full article body shown on the detail screen. */
    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private NewsCategory category;

    /** e.g. "The Daily Star" — optional free-text attribution. */
    @Column(length = 120)
    private String source;

    @Column(name = "source_url", length = 500)
    private String sourceUrl;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @Column(name = "published_at", nullable = false)
    private LocalDateTime publishedAt;

    /** Drafts stay invisible to normal users (admins still see them). */
    @Column(name = "is_published", nullable = false)
    @Builder.Default
    private Boolean isPublished = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    void onCreate() {
        if (id == null) id = UUID.randomUUID().toString();
        if (createdAt == null) createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
