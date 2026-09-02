package com.example.EV_finder_api.controller;

import com.example.EV_finder_api.dto.StationResponse;
import com.example.EV_finder_api.service.StationQueryService;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

/** Public, read-only station discovery (used by the app's Home/Map/Station screens). */
@RestController
@RequestMapping("/api/stations")
public class StationController {

    private final StationQueryService stationQueryService;

    public StationController(StationQueryService stationQueryService) {
        this.stationQueryService = stationQueryService;
    }

    @GetMapping
    public List<StationResponse> list(@RequestParam(required = false) String q) {
        return stationQueryService.search(q);
    }

    @GetMapping("/nearby")
    public List<StationResponse> nearby(@RequestParam BigDecimal latitude,
                                        @RequestParam BigDecimal longitude,
                                        @RequestParam(defaultValue = "10") double radiusKm) {
        return stationQueryService.nearby(latitude, longitude, radiusKm);
    }

    @GetMapping("/{id}")
    public StationResponse details(@PathVariable String id) {
        return stationQueryService.details(id);
    }
}
