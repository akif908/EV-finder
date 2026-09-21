package com.example.EV_finder_api.controller;

import com.example.EV_finder_api.dto.NewsRequest;
import com.example.EV_finder_api.dto.NewsResponse;
import com.example.EV_finder_api.entity.NewsCategory;
import com.example.EV_finder_api.service.NewsService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Energy & fuel news feed (nation-wide, no location filtering).
 * Reads are open to every authenticated user and return published items only;
 * write access is admin-only, enforced here AND visibility-filtered in the service.
 */
@RestController
@RequestMapping("/api/news")
public class NewsController {

    private final NewsService newsService;

    public NewsController(NewsService newsService) {
        this.newsService = newsService;
    }

    /** Optional ?category=GAS_CNG filter. ?includeUnpublished=true is honored for admins only. */
    @GetMapping
    public List<NewsResponse> list(@RequestParam(required = false) NewsCategory category,
                                   @RequestParam(defaultValue = "false") boolean includeUnpublished) {
        return newsService.list(category, includeUnpublished);
    }

    @GetMapping("/{id}")
    public NewsResponse details(@PathVariable String id) {
        return newsService.details(id);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<NewsResponse> create(@Valid @RequestBody NewsRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(newsService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public NewsResponse update(@PathVariable String id, @Valid @RequestBody NewsRequest request) {
        return newsService.update(id, request);
    }

    /** Publish/unpublish toggle (same pattern as operator station status/fuel open). */
    @PutMapping("/{id}/publish")
    @PreAuthorize("hasRole('ADMIN')")
    public NewsResponse setPublished(@PathVariable String id, @RequestParam boolean published) {
        return newsService.setPublished(id, published);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        newsService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
