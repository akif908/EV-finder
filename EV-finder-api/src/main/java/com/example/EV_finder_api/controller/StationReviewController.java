package com.example.EV_finder_api.controller;

import com.example.EV_finder_api.dto.ReviewRequest;
import com.example.EV_finder_api.dto.ReviewResponse;
import com.example.EV_finder_api.service.StationReviewService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Station ratings & reviews: POST/GET /api/stations/{stationId}/reviews */
@RestController
@RequestMapping("/api/stations/{stationId}/reviews")
public class StationReviewController {

    private final StationReviewService reviewService;

    public StationReviewController(StationReviewService reviewService) {
        this.reviewService = reviewService;
    }

    /** Create or update the current user's review. Requires a COMPLETED booking at the station. */
    @PostMapping
    public ReviewResponse upsert(@PathVariable String stationId, @Valid @RequestBody ReviewRequest request) {
        return reviewService.upsert(stationId, request);
    }

    @GetMapping
    public List<ReviewResponse> list(@PathVariable String stationId) {
        return reviewService.list(stationId);
    }
}
