package com.example.EV_finder_api.service.impl;

import com.example.EV_finder_api.dto.StationResponse;
import com.example.EV_finder_api.entity.Station;
import com.example.EV_finder_api.entity.StationStatus;
import com.example.EV_finder_api.exception.ResourceNotFoundException;
import com.example.EV_finder_api.repository.StationRepository;
import com.example.EV_finder_api.service.StationQueryService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class StationQueryServiceImpl implements StationQueryService {

    private static final double EARTH_RADIUS_KM = 6371.0;

    private final StationRepository stationRepository;

    public StationQueryServiceImpl(StationRepository stationRepository) {
        this.stationRepository = stationRepository;
    }

    @Override
    public List<StationResponse> search(String query) {
        List<Station> active = stationRepository.findByStatusOrderByCreatedAtDesc(StationStatus.ACTIVE);
        if (query == null || query.isBlank()) {
            return active.stream().map(StationResponse::from).toList();
        }
        String q = query.toLowerCase().trim();
        return active.stream()
                .filter(s -> (s.getName() != null && s.getName().toLowerCase().contains(q))
                        || (s.getAddress() != null && s.getAddress().toLowerCase().contains(q)))
                .map(StationResponse::from)
                .toList();
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
                .map(StationResponse::from)
                .toList();
    }

    @Override
    public StationResponse details(String stationId) {
        return StationResponse.from(getActiveStation(stationId));
    }

    @Override
    public List<StationResponse> allActive() {
        return stationRepository.findByStatusOrderByCreatedAtDesc(StationStatus.ACTIVE)
                .stream().map(StationResponse::from).toList();
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
