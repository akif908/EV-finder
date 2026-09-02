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
        Integer availableSlots,
        ServiceStatus status
) {
    public static ServiceResponse from(StationService sv) {
        return new ServiceResponse(sv.getId(), sv.getServiceType(), sv.getConnectorType(),
                sv.getPowerKw(), sv.getPricePerUnit(), sv.getAvailableSlots(), sv.getStatus());
    }
}
