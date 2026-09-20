package com.example.EV_finder_api.dto;

import com.example.EV_finder_api.entity.Booking;
import com.example.EV_finder_api.entity.BookingStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record BookingResponse(
        String id,
        String userId,
        String userName,
        String vehicleId,
        String stationId,
        String stationName,
        String serviceId,
        String serviceName,
        LocalDateTime startTime,
        LocalDateTime endTime,
        BookingStatus status,
        BigDecimal amount,
        LocalDateTime createdAt
) {
    public static BookingResponse from(Booking b, BigDecimal amount) {
        return new BookingResponse(
                b.getId(), b.getUser().getId(), b.getUser().getName(), b.getVehicle().getId(),
                b.getStation().getId(), b.getStation().getName(),
                b.getService().getId(), b.getService().getServiceType().name(),
                b.getStartTime(), b.getEndTime(), b.getStatus(),
                amount, b.getCreatedAt());
    }
}
