package com.example.EV_finder_api.service;

import com.example.EV_finder_api.dto.FuelInventoryUpdateRequest;
import com.example.EV_finder_api.dto.FuelStationRequest;
import com.example.EV_finder_api.dto.FuelStationResponse;
import com.example.EV_finder_api.entity.FuelType;

import java.util.List;

/** Operator-side fuel station management. Every method enforces the own-station rule. */
public interface FuelStationManagementService {

    FuelStationResponse create(FuelStationRequest request);

    List<FuelStationResponse> myStations();

    FuelStationResponse update(String fuelStationId, FuelStationRequest request);

    /** Hard delete (inventories cascade with it). */
    void delete(String fuelStationId);

    FuelStationResponse setOpen(String fuelStationId, boolean isOpen);

    /** Add a fuel type or update its queue/stock/price (upsert per fuel type). */
    FuelStationResponse updateFuel(String fuelStationId, FuelType fuelType, FuelInventoryUpdateRequest request);

    void removeFuel(String fuelStationId, FuelType fuelType);
}
