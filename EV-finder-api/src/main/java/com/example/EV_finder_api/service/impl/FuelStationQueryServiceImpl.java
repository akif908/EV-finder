package com.example.EV_finder_api.service.impl;

import com.example.EV_finder_api.dto.FuelStationResponse;
import com.example.EV_finder_api.entity.FuelStation;
import com.example.EV_finder_api.entity.FuelType;
import com.example.EV_finder_api.exception.ResourceNotFoundException;
import com.example.EV_finder_api.repository.FuelStationRepository;
import com.example.EV_finder_api.service.FuelStationQueryService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class FuelStationQueryServiceImpl implements FuelStationQueryService {

    private static final double EARTH_RADIUS_KM = 6371.0;

    private final FuelStationRepository fuelStationRepository;

    public FuelStationQueryServiceImpl(FuelStationRepository fuelStationRepository) {
        this.fuelStationRepository = fuelStationRepository;
    }

    @Override
    public List<FuelStationResponse> search(String query, FuelType fuel, boolean availableOnly) {
        // closed stations stay in the list so users see their "Closed" status
        return fuelStationRepository.findAll().stream()
                .filter(s -> matchesQuery(s, query))
                .filter(s -> fuel == null || s.getInventories().stream().anyMatch(i -> i.getFuelType() == fuel))
                .filter(s -> !availableOnly
                        || s.getInventories().stream().anyMatch(i -> i.getRemainingLiters().signum() > 0))
                .sorted(Comparator.comparing((FuelStation s) -> !Boolean.TRUE.equals(s.getIsOpen()))
                        .thenComparing(FuelStation::getName))
                .map(FuelStationResponse::from)
                .toList();
    }

    @Override
    public List<FuelStationResponse> nearby(BigDecimal latitude, BigDecimal longitude, double radiusKm) {
        double lat = latitude.doubleValue();
        double lng = longitude.doubleValue();
        return fuelStationRepository.findAll().stream()
                .filter(s -> haversineKm(lat, lng,
                        s.getLatitude().doubleValue(), s.getLongitude().doubleValue()) <= radiusKm)
                .sorted(Comparator.comparingDouble(s ->
                        haversineKm(lat, lng, s.getLatitude().doubleValue(), s.getLongitude().doubleValue())))
                .map(FuelStationResponse::from)
                .toList();
    }

    @Override
    public FuelStationResponse details(String fuelStationId) {
        return FuelStationResponse.from(getStation(fuelStationId));
    }

    private FuelStation getStation(String fuelStationId) {
        return fuelStationRepository.findById(fuelStationId)
                .orElseThrow(() -> new ResourceNotFoundException("Fuel station not found: " + fuelStationId));
    }

    private boolean matchesQuery(FuelStation s, String query) {
        if (query == null || query.isBlank()) return true;
        String q = query.toLowerCase().trim();
        return (s.getName() != null && s.getName().toLowerCase().contains(q))
                || (s.getAddress() != null && s.getAddress().toLowerCase().contains(q));
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
