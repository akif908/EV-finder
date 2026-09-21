package com.example.EV_finder_api.repository;

import com.example.EV_finder_api.entity.News;
import com.example.EV_finder_api.entity.NewsCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface NewsRepository extends JpaRepository<News, String> {

    List<News> findByIsPublishedTrueOrderByPublishedAtDesc();

    List<News> findByIsPublishedTrueAndCategoryOrderByPublishedAtDesc(NewsCategory category);

    List<News> findAllByOrderByPublishedAtDesc();

    /** Admin listing (drafts included); single query for the optional category filter. */
    @Query("SELECT n FROM News n " +
           "WHERE (:category IS NULL OR n.category = :category) " +
           "ORDER BY n.publishedAt DESC")
    List<News> findAllForAdmin(@Param("category") NewsCategory category);

    boolean existsByTitle(String title);

    Optional<News> findByTitle(String title);
}
