package com.example.evfinder.core.model

data class AddVehicleRequest(
    val manufacturer: String?,
    val model: String?,
    val vehicleType: String,
    val connectorType: String?,
    val batteryCapacityKwh: Double? = null,
    val registrationNo: String
)
