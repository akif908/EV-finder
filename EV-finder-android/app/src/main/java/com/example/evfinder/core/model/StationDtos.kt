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
    val fuelLevel: Int = 100,     // remaining energy reserve, 0–100 (%); backend always sends it
    val avgRating: Double? = null, // average of station_reviews (null = no reviews yet)
    val reviewCount: Int = 0,
    val services: List<ServiceDto> = emptyList()
)

/** Mirrors the backend ServiceResponse JSON. */
data class ServiceDto(
    val id: String,
    val serviceType: String,   // CHARGING | BATTERY_SWAP
    val connectorType: String?,
    val powerKw: Double?,
    val pricePerUnit: Double,
    val availableSlots: Int,
    val status: String
)
