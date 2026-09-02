package com.example.EV_finder_api.controller;

import com.example.EV_finder_api.dto.SlotResponse;
import com.example.EV_finder_api.service.AvailabilityService;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/services/{serviceId}/slots")
public class AvailabilityController {

    private final AvailabilityService availabilityService;

    public AvailabilityController(AvailabilityService availabilityService) {
        this.availabilityService = availabilityService;
    }

    /** e.g. GET /api/services/{id}/slots?date=2026-09-03 */
    @GetMapping
    public List<SlotResponse> slots(@PathVariable String serviceId,
                                    @RequestParam LocalDate date) {
        return availabilityService.slotsFor(serviceId, date);
    }
}
