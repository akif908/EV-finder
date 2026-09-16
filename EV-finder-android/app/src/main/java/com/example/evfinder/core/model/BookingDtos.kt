package com.example.evfinder.core.model

data class VehicleDto(
    val id: String,
    val manufacturer: String?,
    val model: String?,
    val vehicleType: String,
    val connectorType: String?,
    val batteryCapacityKwh: Double?,
    val registrationNo: String
)

data class SlotDto(
    val startTime: String,
    val endTime: String,
    val capacity: Int,
    val available: Int,
    val bookable: Boolean
)

data class BookingRequestDto(
    val vehicleId: String,
    val serviceId: String,
    val startTime: String,
    val endTime: String
)

data class BookingDto(
    val id: String,
    val userId: String,
    val userName: String? = null,   // shown on the operator side
    val vehicleId: String,
    val stationId: String,
    val stationName: String,
    val serviceId: String,
    val serviceName: String,
    val startTime: String,
    val endTime: String,
    val status: String,          // PENDING | CONFIRMED | COMPLETED | CANCELLED
    val amount: Double?,
    val createdAt: String?
)

data class PaymentRequestDto(
    val paymentMethod: String,
    val forceFailure: Boolean = false
)

data class PaymentDto(
    val paymentId: String,
    val bookingId: String,
    val amount: Double,
    val paymentMethod: String,
    val status: String,          // SUCCESS | FAILED | REFUNDED
    val transactionRef: String?
)
