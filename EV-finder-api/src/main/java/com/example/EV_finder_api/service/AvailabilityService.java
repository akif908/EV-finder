package com.example.EV_finder_api.service;

import com.example.EV_finder_api.dto.SlotResponse;

import java.time.LocalDate;
import java.util.List;

/**
 * Availability ("Option B" from the project context): a service has a fixed
 * concurrent capacity (available_slots); a slot is free while active bookings
 * overlapping it are below that capacity. No explicit slot table.
 */
public interface AvailabilityService {

    /** Hourly slots for one service on one date, bounded by the station's opening hours. */
    List<SlotResponse> slotsFor(String serviceId, LocalDate date);
}
