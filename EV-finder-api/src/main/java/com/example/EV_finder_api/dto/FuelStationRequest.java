package com.example.EV_finder_api.dto;

import com.example.EV_finder_api.entity.FuelStation;
import com.example.EV_finder_api.entity.FuelType;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record FuelStationRequest(
        @NotBlank @Size(max = 120) String name,
        @Size(max = 500) String description,
        @Size(max = 255) String address,
        @NotNull @DecimalMin("-90") @DecimalMax("90") BigDecimal latitude,
        @NotNull @DecimalMin("-180") @DecimalMax("180") BigDecimal longitude,
        @NotNull Boolean isOpen
) {
    public static FuelStationRequest fromEntity(FuelStation s) {
        return new FuelStationRequest(s.getName(), s.getDescription(), s.getAddress(),
                s.getLatitude(), s.getLongitude(), s.getIsOpen());
    }
}
