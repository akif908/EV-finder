package com.example.evfinder.core.model

/** Mirrors backend StationRequest (operator create/update). */
data class OperatorStationRequest(
    val name: String,
    val description: String?,
    val address: String?,
    val latitude: Double,
    val longitude: Double,
    val openingTime: String?,   // "HH:mm"
    val closingTime: String?
)

/** Mirrors backend StationServiceRequest (operator add/update service). */
data class OperatorServiceRequest(
    val serviceType: String,    // CHARGING | BATTERY_SWAP
    val connectorType: String?,
    val powerKw: Double?,
    val pricePerUnit: Double,
    val availableSlots: Int
)
