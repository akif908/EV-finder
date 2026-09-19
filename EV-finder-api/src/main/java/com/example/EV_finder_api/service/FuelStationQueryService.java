package com.example.EV_finder_api.service;

import com.example.EV_finder_api.dto.FuelStationResponse;
import com.example.EV_finder_api.entity.FuelType;

import java.math.BigDecimal;
import java.util.List;

/** Fuel station discovery for regular users. No booking concept — read-only info. */
public interface FuelStationQueryService {

    /**
     * @param fuel          optional fuel-type filter (LPG/DIESEL/OCTANE/PETROL)
     * @param availableOnly true = only stations having at least one fuel type with stock left
     */
    List<FuelStationResponse> search(String query, FuelType fuel, boolean availableOnly);

    List<FuelStationResponse> nearby(BigDecimal latitude, BigDecimal longitude, double radiusKm);

    FuelStationResponse details(String fuelStationId);
}
