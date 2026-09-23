package com.example.EV_finder_api.dto;

import com.example.EV_finder_api.entity.FuelType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/**
 * Operator update of one fuel type's live info (queue / stock / price).
 * Business rules: values must never be negative; liters and BDT are the units.
 */
public record FuelInventoryUpdateRequest(
        @NotNull @Min(0) Integer queueCount,
        @NotNull @DecimalMin("0") BigDecimal remainingLiters,
        @NotNull @DecimalMin("0") BigDecimal pricePerLiter
) {
}
