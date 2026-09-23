package com.example.EV_finder_api.controller;

import com.example.EV_finder_api.dto.NewsResponse;
import com.example.EV_finder_api.service.impl.NewsServiceImpl;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/** Energy / fuel / EV news for the app's "latest updates" section. */
@RestController
@RequestMapping("/api/news")
public class NewsController {

    private final NewsServiceImpl newsService;

    public NewsController(NewsServiceImpl newsService) {
        this.newsService = newsService;
    }

    @GetMapping
    public List<NewsResponse> latest(
            @RequestParam(required = false) String category,
            @RequestParam(defaultValue = "30") int limit) {
        return newsService.latest(category, Math.min(Math.max(limit, 1), 60));
    }

    /** Manual refresh for admins, in case the cache is stale or empty. */
    @PostMapping("/refresh")
    @PreAuthorize("hasRole('ADMIN')")
    public Map<String, Integer> refresh() {
        return Map.of("added", newsService.refresh());
    }
}
