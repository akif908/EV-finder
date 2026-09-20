package com.example.EV_finder_api.dto;

/** Platform-level stats for the admin dashboard. */
public record PlatformOverview(
        long totalUsers,
        long totalOperators,
        long totalStations,
        long activeStations,
        long totalBookings,
        double totalRevenue
) {}
