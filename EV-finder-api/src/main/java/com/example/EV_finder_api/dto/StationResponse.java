package com.example.EV_finder_api.dto;

import com.example.EV_finder_api.entity.ServiceStatus;
import com.example.EV_finder_api.entity.Station;
import com.example.EV_finder_api.entity.StationStatus;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.List;

public record StationResponse(
        String id,
        String name,
        String description,
        String address,
        BigDecimal latitude,
        BigDecimal longitude,
        LocalTime openingTime,
        LocalTime closingTime,
        StationStatus status,
        /** Aggregated review score (0 when never rated) — used to rank stations. */
        double averageRating,
        int reviewCount,
        List<ServiceResponse> services
) {
    public static StationResponse from(Station s) {
        return from(s, 0.0, 0);
    }

    public static StationResponse from(Station s, double averageRating, int reviewCount) {
        return new StationResponse(
                s.getId(), s.getName(), s.getDescription(), s.getAddress(),
                s.getLatitude(), s.getLongitude(), s.getOpeningTime(), s.getClosingTime(),
                s.getStatus(),
                Math.round(averageRating * 10) / 10.0,
                reviewCount,
                s.getServices().stream()
                        .filter(sv -> sv.getStatus() == ServiceStatus.ACTIVE)
                        .map(ServiceResponse::from)
                        .toList());
    }
}
