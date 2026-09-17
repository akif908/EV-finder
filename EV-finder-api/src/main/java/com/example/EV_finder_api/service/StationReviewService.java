package com.example.EV_finder_api.service;

import com.example.EV_finder_api.dto.ReviewRequest;
import com.example.EV_finder_api.dto.ReviewResponse;

import java.util.List;

/** Station ratings & reviews. Writing requires a completed booking at that station. */
public interface StationReviewService {

    /** Create or update the current user's review for a station (one per user per station). */
    ReviewResponse upsert(String stationId, ReviewRequest request);

    List<ReviewResponse> list(String stationId);
}
