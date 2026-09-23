package com.example.EV_finder_api.dto;

import com.example.EV_finder_api.entity.FuelStation;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

public record FuelStationResponse(
        String id,
        String name,
        String description,
        String address,
        BigDecimal latitude,
        BigDecimal longitude,
        Boolean isOpen,
        List<FuelInventoryResponse> inventories,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static FuelStationResponse from(FuelStation s) {
        return new FuelStationResponse(s.getId(), s.getName(), s.getDescription(), s.getAddress(),
                s.getLatitude(), s.getLongitude(), s.getIsOpen(),
                s.getInventories().stream()
                        .sorted(Comparator.comparing(inv -> inv.getFuelType().ordinal()))
                        .map(FuelInventoryResponse::from)
                        .toList(),
                s.getCreatedAt(), s.getUpdatedAt());
    }
}
