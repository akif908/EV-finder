package com.example.EV_finder_api.service;

import com.example.EV_finder_api.dto.NewsRequest;
import com.example.EV_finder_api.dto.NewsResponse;
import com.example.EV_finder_api.entity.NewsCategory;

import java.util.List;

/**
 * Energy & fuel news feed. Same nation-wide list for every user (no location
 * filtering by design). Only published items are visible to non-admin users;
 * admins may additionally list/manage drafts.
 */
public interface NewsService {

    /**
     * Published feed for everyone; {@code includeUnpublished} additionally
     * returns drafts but is honored for ADMIN requests only.
     */
    List<NewsResponse> list(NewsCategory category, boolean includeUnpublished);

    /** Published item for users; drafts resolve only for admins (else 404). */
    NewsResponse details(String newsId);

    NewsResponse create(NewsRequest request);

    NewsResponse update(String newsId, NewsRequest request);

    NewsResponse setPublished(String newsId, boolean published);

    void delete(String newsId);
}
