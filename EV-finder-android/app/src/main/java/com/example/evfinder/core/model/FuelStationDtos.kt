package com.example.evfinder.core.model

/**
 * Fuel Station module — completely separate from EV StationDto.
 * Mirrors backend FuelStationResponse JSON (see FuelStationResponse.java).
 */
data class FuelStationDto(
    val id: String,
    val name: String,
    val description: String?,
    val address: String?,
    val latitude: Double,
    val longitude: Double,
    val isOpen: Boolean = true,
    val inventories: List<FuelInventoryDto> = emptyList(),
    val createdAt: String? = null,
    val updatedAt: String? = null
)

/** Live info for one fuel type: queue, remaining stock (liters), price (BDT/L). */
data class FuelInventoryDto(
    val fuelType: String,           // LPG | DIESEL | OCTANE | PETROL
    val queueCount: Int = 0,
    val remainingLiters: Double = 0.0,
    val pricePerLiter: Double = 0.0,
    val updatedAt: String? = null
)

/** Mirrors backend FuelStationRequest (operator create/update). */
data class FuelStationRequest(
    val name: String,
    val description: String?,
    val address: String?,
    val latitude: Double,
    val longitude: Double,
    val isOpen: Boolean
)

/** Mirrors backend FuelInventoryUpdateRequest (queue/stock/price for one fuel type). */
data class FuelInventoryUpdateRequest(
    val queueCount: Int,
    val remainingLiters: Double,
    val pricePerLiter: Double
)
