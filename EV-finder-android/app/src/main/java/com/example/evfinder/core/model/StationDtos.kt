package com.example.evfinder.core.model

/** Mirrors the backend StationResponse JSON (see StationResponse.java). */
data class StationDto(
    val id: String,
    val name: String,
    val description: String?,
    val address: String?,
    val latitude: Double,
    val longitude: Double,
    val openingTime: String?,
    val closingTime: String?,
    val status: String,
    /** Aggregated review score (0 when never rated) — stations rank by this. */
    val averageRating: Double = 0.0,
    val reviewCount: Int = 0,
    /** Remaining energy reserve at the station, 0–100 (%) — from the branch. */
    val fuelLevel: Int = 100,
    val services: List<ServiceDto> = emptyList()
)

/** Mirrors the backend ServiceResponse JSON. */
data class ServiceDto(
    val id: String,
    val serviceType: String,   // CHARGING | BATTERY_SWAP
    val connectorType: String?,
    val powerKw: Double?,
    val pricePerUnit: Double,
    /** Slots free right now (capacity minus bookings happening now). */
    val availableSlots: Int,
    /** Installed capacity — denominator for the availability bar. */
    val capacitySlots: Int? = null,
    val status: String
)
