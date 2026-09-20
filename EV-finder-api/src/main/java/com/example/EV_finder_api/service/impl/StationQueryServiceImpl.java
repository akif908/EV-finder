package com.example.EV_finder_api.service.impl;

import com.example.EV_finder_api.dto.StationResponse;
import com.example.EV_finder_api.entity.Review;
import com.example.EV_finder_api.entity.Station;
import com.example.EV_finder_api.entity.StationStatus;
import com.example.EV_finder_api.exception.ResourceNotFoundException;
import com.example.EV_finder_api.repository.ReviewRepository;
import com.example.EV_finder_api.repository.StationRepository;
import com.example.EV_finder_api.service.StationQueryService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;

/**
 * Public station discovery. Stations are ranked by average review score
 * (highest first), so the best-rated stations surface at the top of the app.
 */
@Service
@Transactional(readOnly = true)
public class StationQueryServiceImpl implements StationQueryService {

    private static final double EARTH_RADIUS_KM = 6371.0;

    private final StationRepository stationRepository;
    private final ReviewRepository reviewRepository;

    public StationQueryServiceImpl(StationRepository stationRepository,
                                   ReviewRepository reviewRepository) {
        this.stationRepository = stationRepository;
        this.reviewRepository = reviewRepository;
    }

    @Override
    public List<StationResponse> search(String query) {
        List<Station> active = stationRepository.findByStatusOrderByCreatedAtDesc(StationStatus.ACTIVE);
        List<StationResponse> responses;
        if (query == null || query.isBlank()) {
            responses = active.stream().map(this::withRating).toList();
        } else {
            String q = query.toLowerCase().trim();
            responses = active.stream()
                    .filter(s -> (s.getName() != null && s.getName().toLowerCase().contains(q))
                            || (s.getAddress() != null && s.getAddress().toLowerCase().contains(q)))
                    .map(this::withRating)
                    .toList();
        }
        return rankByRating(responses);
    }

    @Override
    public List<StationResponse> nearby(BigDecimal latitude, BigDecimal longitude, double radiusKm) {
        double lat = latitude.doubleValue();
        double lng = longitude.doubleValue();
        return stationRepository.findByStatusOrderByCreatedAtDesc(StationStatus.ACTIVE).stream()
                .filter(s -> haversineKm(lat, lng,
                        s.getLatitude().doubleValue(), s.getLongitude().doubleValue()) <= radiusKm)
                .sorted(Comparator.comparingDouble(s ->
                        haversineKm(lat, lng, s.getLatitude().doubleValue(), s.getLongitude().doubleValue())))
                .map(this::withRating)
                .toList();
    }

    @Override
    public StationResponse details(String stationId) {
        return withRating(getActiveStation(stationId));
    }

    @Override
    public List<StationResponse> allActive() {
        return rankByRating(stationRepository.findByStatusOrderByCreatedAtDesc(StationStatus.ACTIVE)
                .stream().map(this::withRating).toList());
    }

    /** Highest average rating first; unrated stations keep their original order last. */
    private List<StationResponse> rankByRating(List<StationResponse> stations) {
        return stations.stream()
                .sorted(Comparator
                        .comparingDouble(StationResponse::averageRating).reversed()
                        .thenComparing(Comparator.comparingInt(StationResponse::reviewCount).reversed())
                        .thenComparing(StationResponse::name))
                .toList();
    }

    private StationResponse withRating(Station station) {
        List<Review> reviews = reviewRepository.findByStationIdOrderByCreatedAtDesc(station.getId());
        double avg = reviews.stream().mapToInt(Review::getRating).average().orElse(0.0);
        return StationResponse.from(station, avg, reviews.size());
    }

    private Station getActiveStation(String stationId) {
        Station station = stationRepository.findById(stationId)
                .orElseThrow(() -> new ResourceNotFoundException("Station not found: " + stationId));
        if (station.getStatus() == StationStatus.INACTIVE) {
            throw new ResourceNotFoundException("Station not available: " + stationId);
        }
        return station;
    }

    static double haversineKm(double lat1, double lng1, double lat2, double lng2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        return EARTH_RADIUS_KM * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }
}
