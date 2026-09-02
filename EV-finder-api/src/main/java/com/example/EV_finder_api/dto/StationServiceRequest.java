package com.example.EV_finder_api.dto;

import com.example.EV_finder_api.entity.ServiceType;
import com.example.EV_finder_api.entity.StationService;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record StationServiceRequest(
        @NotNull ServiceType serviceType,
        @Size(max = 40) String connectorType,
        @DecimalMin("0") BigDecimal powerKw,
        @NotNull @DecimalMin("0") BigDecimal pricePerUnit,
        @NotNull @Min(1) @Max(100) Integer availableSlots
) {
    public static StationServiceRequest fromEntity(StationService sv) {
        return new StationServiceRequest(sv.getServiceType(), sv.getConnectorType(),
                sv.getPowerKw(), sv.getPricePerUnit(), sv.getAvailableSlots());
    }
}
