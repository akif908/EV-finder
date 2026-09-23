package com.example.EV_finder_api.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * A cached energy / fuel / EV news headline pulled from a free public RSS feed.
 * Cached rather than proxied per request so the app is fast and still useful
 * when the upstream feed is briefly unreachable.
 */
@Entity
@Table(name = "news_articles",
        uniqueConstraints = @UniqueConstraint(name = "uq_news_link", columnNames = "link"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NewsArticle {

    @Id
    @Column(name = "news_id", length = 36)
    private String id;

    @Column(nullable = false, length = 400)
    private String title;

    /** Canonical article URL — also the de-duplication key. */
    @Column(nullable = false, length = 600)
    private String link;

    /** Publishing outlet, e.g. "The Daily Star". */
    @Column(length = 150)
    private String source;

    /** EV | FUEL | LPG | POLICY | GENERAL */
    @Column(nullable = false, length = 20)
    private String category;

    @Column(length = 500)
    private String summary;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    @Column(name = "fetched_at", nullable = false)
    private LocalDateTime fetchedAt;

    @PrePersist
    void onCreate() {
        if (id == null) id = UUID.randomUUID().toString();
        if (fetchedAt == null) fetchedAt = LocalDateTime.now();
    }
}
