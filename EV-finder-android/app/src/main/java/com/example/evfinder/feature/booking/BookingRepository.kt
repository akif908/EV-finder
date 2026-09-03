package com.example.evfinder.feature.booking

import com.example.evfinder.core.model.*
import com.example.evfinder.core.network.ApiClient
import com.example.evfinder.core.network.ApiService
import org.json.JSONObject
import retrofit2.Response
import java.io.IOException

/** Booking + payment + vehicle calls for the booking flow. */
class BookingRepository {

    private val api: ApiService = ApiClient.retrofit.create(ApiService::class.java)

    suspend fun myVehicles(): Result<List<VehicleDto>> = handle { api.getMyVehicles() }

    suspend fun addVehicle(
        vehicleType: String, registrationNo: String,
        manufacturer: String?, model: String?, connectorType: String?
    ): Result<VehicleDto> = handle {
        api.addVehicle(AddVehicleRequest(manufacturer, model, vehicleType, connectorType, null, registrationNo))
    }

    suspend fun slots(serviceId: String, date: String): Result<List<SlotDto>> =
        handle { api.getSlots(serviceId, date) }

    suspend fun createBooking(vehicleId: String, serviceId: String, start: String, end: String): Result<BookingDto> =
        handle { api.createBooking(BookingRequestDto(vehicleId, serviceId, start, end)) }

    suspend fun myBookings(status: String? = null): Result<List<BookingDto>> =
        handle { api.getMyBookings(status) }

    suspend fun cancelBooking(id: String): Result<BookingDto> = handle { api.cancelBooking(id) }

    suspend fun pay(bookingId: String, method: String, forceFailure: Boolean): Result<PaymentDto> =
        handle { api.pay(bookingId, PaymentRequestDto(method, forceFailure)) }

    private suspend fun <T> handle(call: suspend () -> Response<T>): Result<T> {
        return try {
            val response = call()
            val body = response.body()
            if (response.isSuccessful && body != null) Result.success(body)
            else Result.failure(Exception(errorMessage(response)))
        } catch (e: IOException) {
            Result.failure(Exception("Cannot reach the server. Is the backend running?"))
        }
    }

    private fun <T> errorMessage(response: Response<T>): String {
        val serverMessage = try {
            JSONObject(response.errorBody()?.string() ?: "{}")
                .optString("message", "").takeIf { it.isNotBlank() }
        } catch (e: Exception) { null }
        return when (response.code()) {
            401 -> serverMessage ?: "Session expired — please log in again"
            403 -> serverMessage ?: "You don't have access to this"
            else -> serverMessage ?: "Request failed (HTTP ${response.code()})"
        }
    }
}
