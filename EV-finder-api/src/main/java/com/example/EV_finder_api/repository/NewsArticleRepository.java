package com.example.EV_finder_api.repository;

import com.example.EV_finder_api.entity.NewsArticle;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface NewsArticleRepository extends JpaRepository<NewsArticle, String> {
    List<NewsArticle> findTop60ByOrderByPublishedAtDesc();
    List<NewsArticle> findTop40ByCategoryOrderByPublishedAtDesc(String category);
    Optional<NewsArticle> findByLink(String link);
    Optional<NewsArticle> findFirstByOrderByFetchedAtDesc();
}
