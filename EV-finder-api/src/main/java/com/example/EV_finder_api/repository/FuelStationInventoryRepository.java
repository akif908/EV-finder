package com.example.EV_finder_api.repository;

import com.example.EV_finder_api.entity.FuelStationInventory;
import com.example.EV_finder_api.entity.FuelType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface FuelStationInventoryRepository extends JpaRepository<FuelStationInventory, String> {

    Optional<FuelStationInventory> findByStationIdAndFuelType(String stationId, FuelType fuelType);

    /**
     * Latest updated inventory row for a fuel type across all stations — used by
     * the news feed to render the current pump price ({DIESEL}, {PETROL}, …
     * tokens in demo articles) without duplicating fuel-price data anywhere.
     */
    Optional<FuelStationInventory> findFirstByFuelTypeOrderByUpdatedAtDesc(FuelType fuelType);
}
