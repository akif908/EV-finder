package com.example.EV_finder_api.dto;

import com.example.EV_finder_api.entity.FuelStationInventory;
import com.example.EV_finder_api.entity.FuelType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record FuelInventoryResponse(
        FuelType fuelType,
        Integer queueCount,
        BigDecimal remainingLiters,
        BigDecimal pricePerLiter,
        LocalDateTime updatedAt
) {
    public static FuelInventoryResponse from(FuelStationInventory inv) {
        return new FuelInventoryResponse(inv.getFuelType(), inv.getQueueCount(),
                inv.getRemainingLiters(), inv.getPricePerLiter(), inv.getUpdatedAt());
    }
}
