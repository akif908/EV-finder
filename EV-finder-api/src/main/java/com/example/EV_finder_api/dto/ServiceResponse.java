package com.example.EV_finder_api.dto;

import com.example.EV_finder_api.entity.ServiceStatus;
import com.example.EV_finder_api.entity.ServiceType;
import com.example.EV_finder_api.entity.StationService;

import java.math.BigDecimal;

public record ServiceResponse(
        String id,
        ServiceType serviceType,
        String connectorType,
        BigDecimal powerKw,
        BigDecimal pricePerUnit,
        /** Slots free right now (capacity minus active bookings covering now). */
        Integer availableSlots,
        /** Installed capacity — the denominator for the client's availability bar. */
        Integer capacitySlots,
        ServiceStatus status
) {
    public static ServiceResponse from(StationService sv) {
        return from(sv, sv.getAvailableSlots());
    }

    public static ServiceResponse from(StationService sv, int liveAvailable) {
        int capacity = sv.getAvailableSlots();
        return new ServiceResponse(sv.getId(), sv.getServiceType(), sv.getConnectorType(),
                sv.getPowerKw(), sv.getPricePerUnit(), liveAvailable, capacity, sv.getStatus());
    }
}
