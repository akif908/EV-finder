package com.example.EV_finder_api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record BookingRequest(
        @NotBlank String vehicleId,
        @NotBlank String serviceId,
        /** UTC/ISO timestamp, e.g. 2026-09-03T14:00:00. Must align to an hourly slot. */
        @NotNull LocalDateTime startTime,
        @NotNull LocalDateTime endTime
) {}
