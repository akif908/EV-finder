package com.example.EV_finder_api.controller;

import com.example.EV_finder_api.dto.FuelStationResponse;
import com.example.EV_finder_api.entity.FuelType;
import com.example.EV_finder_api.service.FuelStationQueryService;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

/**
 * Fuel station discovery (LPG/Diesel/Octane/Petrol). Read-only, no booking.
 * Query params: q (name/address), fuel (LPG|DIESEL|OCTANE|PETROL), availableOnly=true.
 */
@RestController
@RequestMapping("/api/fuel-stations")
public class FuelStationController {

    private final FuelStationQueryService fuelStationQueryService;

    public FuelStationController(FuelStationQueryService fuelStationQueryService) {
        this.fuelStationQueryService = fuelStationQueryService;
    }

    @GetMapping
    public List<FuelStationResponse> list(@RequestParam(required = false) String q,
                                          @RequestParam(required = false) FuelType fuel,
                                          @RequestParam(defaultValue = "false") boolean availableOnly) {
        return fuelStationQueryService.search(q, fuel, availableOnly);
    }

    @GetMapping("/nearby")
    public List<FuelStationResponse> nearby(@RequestParam BigDecimal latitude,
                                            @RequestParam BigDecimal longitude,
                                            @RequestParam(defaultValue = "10") double radiusKm) {
        return fuelStationQueryService.nearby(latitude, longitude, radiusKm);
    }

    @GetMapping("/{id}")
    public FuelStationResponse details(@PathVariable String id) {
        return fuelStationQueryService.details(id);
    }
}
