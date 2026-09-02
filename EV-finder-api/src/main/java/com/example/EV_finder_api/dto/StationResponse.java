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
        List<ServiceResponse> services
) {
    public static StationResponse from(Station s) {
        return new StationResponse(s.getId(), s.getName(), s.getDescription(), s.getAddress(),
                s.getLatitude(), s.getLongitude(), s.getOpeningTime(), s.getClosingTime(),
                s.getStatus(),
                s.getServices().stream()
                        .filter(sv -> sv.getStatus() == com.example.EV_finder_api.entity.ServiceStatus.ACTIVE)
                        .map(ServiceResponse::from)
                        .toList());
    }
}
