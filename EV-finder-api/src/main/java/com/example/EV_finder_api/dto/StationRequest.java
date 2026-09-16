package com.example.EV_finder_api.dto;

import com.example.EV_finder_api.entity.Station;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalTime;

public record StationRequest(
        @NotBlank @Size(max = 120) String name,
        @Size(max = 500) String description,
        @Size(max = 255) String address,
        @NotNull @DecimalMin("-90") @DecimalMax("90") BigDecimal latitude,
        @NotNull @DecimalMin("-180") @DecimalMax("180") BigDecimal longitude,
        LocalTime openingTime,
        LocalTime closingTime,
        /** Optional; defaults to 100 when omitted. */
        @Min(0) @Max(100) Integer fuelLevel
) {
    public static StationRequest fromEntity(Station s) {
        return new StationRequest(s.getName(), s.getDescription(), s.getAddress(),
                s.getLatitude(), s.getLongitude(), s.getOpeningTime(), s.getClosingTime(),
                s.getFuelLevel());
    }
}
