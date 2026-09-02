package com.example.EV_finder_api.dto;

/** One hourly slot for a service on a given date. */
public record SlotResponse(
        String startTime,      // ISO-8601, e.g. 2026-09-03T14:00
        String endTime,
        int capacity,
        int available,
        boolean bookable
) {}
