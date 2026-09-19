package com.example.evfinder.feature.fuel

import com.example.evfinder.core.model.FuelInventoryUpdateRequest
import com.example.evfinder.core.model.FuelStationDto
import com.example.evfinder.core.model.FuelStationRequest
import com.example.evfinder.core.network.ApiClient
import com.example.evfinder.core.network.ApiService
import org.json.JSONObject
import retrofit2.Response
import java.io.IOException

/** Fuel station module (separate from EV stations) — user discovery + operator management. */
class FuelRepository {

    private val api: ApiService = ApiClient.retrofit.create(ApiService::class.java)

    // ---- User: discovery (no booking anywhere in this module) ----

    suspend fun getFuelStations(
        query: String? = null,
        fuel: String? = null,
        availableOnly: Boolean = false
    ): Result<List<FuelStationDto>> =
        handle { api.getFuelStations(query?.takeIf { it.isNotBlank() }, fuel, availableOnly) }

    suspend fun getFuelStation(id: String): Result<FuelStationDto> =
        handle { api.getFuelStation(id) }

    // ---- Operator: management ----

    suspend fun myFuelStations(): Result<List<FuelStationDto>> =
        handle { api.operatorMyFuelStations() }

    suspend fun createFuelStation(body: FuelStationRequest): Result<FuelStationDto> =
        handle { api.operatorCreateFuelStation(body) }

    suspend fun updateFuelStation(id: String, body: FuelStationRequest): Result<FuelStationDto> =
        handle { api.operatorUpdateFuelStation(id, body) }

    suspend fun deleteFuelStation(id: String): Result<Unit> =
        handle { api.operatorDeleteFuelStation(id) }

    suspend fun setOpen(id: String, isOpen: Boolean): Result<FuelStationDto> =
        handle { api.operatorSetFuelStationOpen(id, isOpen) }

    suspend fun updateFuelInventory(
        stationId: String, fuelType: String,
        queueCount: Int, remainingLiters: Double, pricePerLiter: Double
    ): Result<FuelStationDto> = handle {
        api.operatorUpdateFuelInventory(stationId, fuelType, FuelInventoryUpdateRequest(queueCount, remainingLiters, pricePerLiter))
    }

    suspend fun removeFuelType(stationId: String, fuelType: String): Result<Unit> =
        handle { api.operatorRemoveFuelType(stationId, fuelType) }

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
            403 -> serverMessage ?: "You can only manage your own fuel stations"
            else -> serverMessage ?: "Request failed (HTTP ${response.code()})"
        }
    }
}
