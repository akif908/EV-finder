package com.example.evfinder.feature.vehicle

import com.example.evfinder.core.model.AddVehicleRequest
import com.example.evfinder.core.model.VehicleDto
import com.example.evfinder.core.network.ApiClient
import com.example.evfinder.core.network.ApiService
import org.json.JSONObject
import retrofit2.Response
import java.io.IOException

class VehiclesRepository {
    private val api: ApiService = ApiClient.retrofit.create(ApiService::class.java)

    suspend fun myVehicles(): Result<List<VehicleDto>> = handle { api.getMyVehicles() }

    suspend fun addVehicle(
        vehicleType: String, registrationNo: String,
        manufacturer: String?, model: String?, connectorType: String?, batteryCapacityKwh: Double?
    ): Result<VehicleDto> = handle {
        api.addVehicle(AddVehicleRequest(manufacturer, model, vehicleType, connectorType, batteryCapacityKwh, registrationNo))
    }

    suspend fun updateVehicle(
        id: String, vehicleType: String, registrationNo: String,
        manufacturer: String?, model: String?, connectorType: String?, batteryCapacityKwh: Double?
    ): Result<VehicleDto> = handle {
        api.updateVehicle(id, AddVehicleRequest(manufacturer, model, vehicleType, connectorType, batteryCapacityKwh, registrationNo))
    }

    suspend fun deleteVehicle(id: String): Result<Unit> = handle { api.deleteVehicle(id) }

    private suspend fun <T> handle(call: suspend () -> Response<T>): Result<T> {
        return try {
            val response = call()
            val body = response.body()
            if (response.isSuccessful && body != null) Result.success(body)
            else if (response.isSuccessful) @Suppress("UNCHECKED_CAST") Result.success(Unit as T)
            else Result.failure(Exception(errorMessage(response)))
        } catch (e: IOException) {
            Result.failure(Exception("Cannot reach the server. Is the backend running?"))
        }
    }

    private fun <T> errorMessage(response: Response<T>): String {
        val serverMessage = try {
            JSONObject(response.errorBody()?.string() ?: "{}").optString("message", "").takeIf { it.isNotBlank() }
        } catch (e: Exception) { null }
        return when (response.code()) {
            401 -> serverMessage ?: "Session expired — please log in again"
            403 -> serverMessage ?: "You can only manage your own vehicles"
            else -> serverMessage ?: "Request failed (HTTP ${response.code()})"
        }
    }
}
