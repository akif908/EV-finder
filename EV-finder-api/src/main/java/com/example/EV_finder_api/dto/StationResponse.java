package com.example.EV_finder_api.dto;

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
        Integer fuelLevel,
        Double avgRating,
        Integer reviewCount,
        List<ServiceResponse> services
) {
    /** Full detail incl. rating aggregates (see StationReviewRepository.aggregateByStation). */
    public static StationResponse from(Station s, Double avgRating, Integer reviewCount) {
        // fuelLevel is never null in responses: rows created before the column
        // existed are treated as a full reserve.
        Integer fuelLevel = s.getFuelLevel() == null ? 100 : s.getFuelLevel();
        return new StationResponse(s.getId(), s.getName(), s.getDescription(), s.getAddress(),
                s.getLatitude(), s.getLongitude(), s.getOpeningTime(), s.getClosingTime(),
                s.getStatus(), fuelLevel, avgRating, reviewCount == null ? 0 : reviewCount,
                s.getServices().stream()
                        .filter(sv -> sv.getStatus() == com.example.EV_finder_api.entity.ServiceStatus.ACTIVE)
                        .map(ServiceResponse::from)
                        .toList());
    }

    /** Management/creation flows that don't compute rating aggregates. */
    public static StationResponse from(Station s) {
        return from(s, null, 0);
    }
}
