package com.example.evfinder.core.model

data class OperatorStationRequest(
    val name: String,
    val description: String?,
    val address: String?,
    val latitude: Double,
    val longitude: Double,
    val openingTime: String?,
    val closingTime: String?
)

data class OperatorServiceRequest(
    val serviceType: String,
    val connectorType: String?,
    val powerKw: Double?,
    val pricePerUnit: Double,
    val availableSlots: Int
)

data class AdminOverviewDto(
    val totalUsers: Long,
    val totalOperators: Long,
    val totalStations: Long,
    val activeStations: Long,
    val totalBookings: Long,
    val totalRevenue: Double
)

data class AdminUserDto(
    val id: String,
    val name: String,
    val email: String,
    val role: String,
    val createdAt: String?
)
