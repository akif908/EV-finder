package com.example.EV_finder_api.dto;

import com.example.EV_finder_api.entity.ServiceStatus;
import com.example.EV_finder_api.entity.Station;
import com.example.EV_finder_api.entity.StationService;
import com.example.EV_finder_api.entity.StationStatus;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

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
        /** Remaining energy reserve at the station, 0-100 (%). */
        Integer fuelLevel,
        /** Aggregated review score (0 when never rated) — used to rank stations. */
        double averageRating,
        int reviewCount,
        List<ServiceResponse> services
) {
    public static StationResponse from(Station s) {
        return from(s, 0.0, 0);
    }

    public static StationResponse from(Station s, double averageRating, int reviewCount) {
        return from(s, averageRating, reviewCount, Map.of());
    }

    /**
     * @param activeBookingsNow serviceId → bookings whose window covers "now";
     *                          used to derive live availability per service.
     */
    public static StationResponse from(Station s, double averageRating, int reviewCount,
                                       Map<String, Long> activeBookingsNow) {
        return new StationResponse(
                s.getId(), s.getName(), s.getDescription(), s.getAddress(),
                s.getLatitude(), s.getLongitude(), s.getOpeningTime(), s.getClosingTime(),
                s.getStatus(),
                s.getFuelLevel(),
                Math.round(averageRating * 10) / 10.0,
                reviewCount,
                s.getServices().stream()
                        .filter(sv -> sv.getStatus() == ServiceStatus.ACTIVE)
                        .map(sv -> ServiceResponse.from(sv, liveAvailable(sv, activeBookingsNow)))
                        .toList());
    }

    private static int liveAvailable(StationService sv, Map<String, Long> activeBookingsNow) {
        long busy = activeBookingsNow.getOrDefault(sv.getId(), 0L);
        return (int) Math.max(0, sv.getAvailableSlots() - busy);
    }
}
