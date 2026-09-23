package com.example.EV_finder_api.service.impl;

import com.example.EV_finder_api.dto.StationResponse;
import com.example.EV_finder_api.entity.Review;
import com.example.EV_finder_api.entity.Station;
import com.example.EV_finder_api.entity.StationStatus;
import com.example.EV_finder_api.exception.ResourceNotFoundException;
import com.example.EV_finder_api.repository.BookingRepository;
import com.example.EV_finder_api.repository.ReviewRepository;
import com.example.EV_finder_api.repository.StationRepository;
import com.example.EV_finder_api.service.StationQueryService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Public station discovery. Stations are ranked by average review score
 * (highest first), so the best-rated stations surface at the top of the app.
 * Each response also carries live per-service availability (capacity minus
 * bookings happening now) for the client's availability bars.
 */
@Service
@Transactional(readOnly = true)
public class StationQueryServiceImpl implements StationQueryService {

    private static final double EARTH_RADIUS_KM = 6371.0;

    private final StationRepository stationRepository;
    private final ReviewRepository reviewRepository;
    private final BookingRepository bookingRepository;

    public StationQueryServiceImpl(StationRepository stationRepository,
                                   ReviewRepository reviewRepository,
                                   BookingRepository bookingRepository) {
        this.stationRepository = stationRepository;
        this.reviewRepository = reviewRepository;
        this.bookingRepository = bookingRepository;
    }

    @Override
    public List<StationResponse> search(String query) {
        List<Station> active = stationRepository.findByStatusOrderByCreatedAtDesc(StationStatus.ACTIVE);
        Map<String, Long> usage = activeBookingsNow();
        List<StationResponse> responses;
        if (query == null || query.isBlank()) {
            responses = active.stream().map(s -> withRating(s, usage)).toList();
        } else {
            String q = query.toLowerCase().trim();
            responses = active.stream()
                    .filter(s -> (s.getName() != null && s.getName().toLowerCase().contains(q))
                            || (s.getAddress() != null && s.getAddress().toLowerCase().contains(q)))
                    .map(s -> withRating(s, usage))
                    .toList();
        }
        return rankByRating(responses);
    }

    @Override
    public List<StationResponse> nearby(BigDecimal latitude, BigDecimal longitude, double radiusKm) {
        double lat = latitude.doubleValue();
        double lng = longitude.doubleValue();
        Map<String, Long> usage = activeBookingsNow();
        return stationRepository.findByStatusOrderByCreatedAtDesc(StationStatus.ACTIVE).stream()
                .filter(s -> haversineKm(lat, lng,
                        s.getLatitude().doubleValue(), s.getLongitude().doubleValue()) <= radiusKm)
                .sorted(Comparator.comparingDouble(s ->
                        haversineKm(lat, lng, s.getLatitude().doubleValue(), s.getLongitude().doubleValue())))
                .map(s -> withRating(s, usage))
                .toList();
    }

    @Override
    public StationResponse details(String stationId) {
        return withRating(getActiveStation(stationId));
    }

    @Override
    public List<StationResponse> allActive() {
        Map<String, Long> usage = activeBookingsNow();
        return rankByRating(stationRepository.findByStatusOrderByCreatedAtDesc(StationStatus.ACTIVE)
                .stream().map(s -> withRating(s, usage)).toList());
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
        return withRating(station, activeBookingsNow());
    }

    private StationResponse withRating(Station station, Map<String, Long> activeBookingsNow) {
        List<Review> reviews = reviewRepository.findByStationIdOrderByCreatedAtDesc(station.getId());
        double avg = reviews.stream().mapToInt(Review::getRating).average().orElse(0.0);
        return StationResponse.from(station, avg, reviews.size(), activeBookingsNow);
    }

    /** serviceId → count of PENDING/CONFIRMED bookings covering the current instant. */
    private Map<String, Long> activeBookingsNow() {
        return bookingRepository.countActiveNowGrouped(LocalDateTime.now()).stream()
                .collect(Collectors.toMap(row -> (String) row[0], row -> (Long) row[1]));
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
