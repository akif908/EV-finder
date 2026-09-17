package com.example.EV_finder_api.service.impl;

import com.example.EV_finder_api.dto.StationResponse;
import com.example.EV_finder_api.entity.Station;
import com.example.EV_finder_api.entity.StationStatus;
import com.example.EV_finder_api.exception.ResourceNotFoundException;
import com.example.EV_finder_api.repository.StationRepository;
import com.example.EV_finder_api.repository.StationReviewRepository;
import com.example.EV_finder_api.service.StationQueryService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional(readOnly = true)
public class StationQueryServiceImpl implements StationQueryService {

    private static final double EARTH_RADIUS_KM = 6371.0;

    private final StationRepository stationRepository;
    private final StationReviewRepository reviewRepository;

    public StationQueryServiceImpl(StationRepository stationRepository,
                                   StationReviewRepository reviewRepository) {
        this.stationRepository = stationRepository;
        this.reviewRepository = reviewRepository;
    }

    /** stationId -> average rating for enriching responses; stations without reviews are absent. */
    private Map<String, Double> avgRatings() {
        Map<String, Double> map = new HashMap<>();
        for (Object[] row : reviewRepository.aggregateByStation()) {
            map.put((String) row[0], (Double) row[1]);
        }
        return map;
    }

    private long reviewCount(String stationId) {
        return reviewRepository.countByStationId(stationId);
    }

    private StationResponse toResponse(Station s, Map<String, Double> ratings) {
        Double avg = ratings.get(s.getId());
        return StationResponse.from(s, avg, avg == null ? 0 : (int) reviewCount(s.getId()));
    }

    @Override
    public List<StationResponse> search(String query) {
        List<Station> active = stationRepository.findByStatusOrderByCreatedAtDesc(StationStatus.ACTIVE);
        Map<String, Double> ratings = avgRatings();
        if (query == null || query.isBlank()) {
            return active.stream().map(s -> toResponse(s, ratings)).toList();
        }
        String q = query.toLowerCase().trim();
        return active.stream()
                .filter(s -> (s.getName() != null && s.getName().toLowerCase().contains(q))
                        || (s.getAddress() != null && s.getAddress().toLowerCase().contains(q)))
                .map(s -> toResponse(s, ratings))
                .toList();
    }

    @Override
    public List<StationResponse> nearby(BigDecimal latitude, BigDecimal longitude, double radiusKm) {
        double lat = latitude.doubleValue();
        double lng = longitude.doubleValue();
        Map<String, Double> ratings = avgRatings();
        return stationRepository.findByStatusOrderByCreatedAtDesc(StationStatus.ACTIVE).stream()
                .filter(s -> haversineKm(lat, lng,
                        s.getLatitude().doubleValue(), s.getLongitude().doubleValue()) <= radiusKm)
                .sorted(Comparator.comparingDouble(s ->
                        haversineKm(lat, lng, s.getLatitude().doubleValue(), s.getLongitude().doubleValue())))
                .map(s -> toResponse(s, ratings))
                .toList();
    }

    @Override
    public StationResponse details(String stationId) {
        return toResponse(getActiveStation(stationId), avgRatings());
    }

    @Override
    public List<StationResponse> allActive() {
        Map<String, Double> ratings = avgRatings();
        return stationRepository.findByStatusOrderByCreatedAtDesc(StationStatus.ACTIVE)
                .stream().map(s -> toResponse(s, ratings)).toList();
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
