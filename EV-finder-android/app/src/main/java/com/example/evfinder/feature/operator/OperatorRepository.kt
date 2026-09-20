package com.example.evfinder.feature.operator

import com.example.evfinder.core.model.BookingDto
import com.example.evfinder.core.model.OperatorServiceRequest
import com.example.evfinder.core.model.OperatorStationRequest
import com.example.evfinder.core.model.ServiceDto
import com.example.evfinder.core.model.StationDto
import com.example.evfinder.core.network.ApiClient
import com.example.evfinder.core.network.ApiService
import org.json.JSONObject
import retrofit2.Response
import java.io.IOException

class OperatorRepository {
    private val api: ApiService = ApiClient.retrofit.create(ApiService::class.java)

    suspend fun myStations(): Result<List<StationDto>> = handle { api.operatorMyStations() }
    suspend fun createStation(body: OperatorStationRequest): Result<StationDto> = handle { api.operatorCreateStation(body) }
    suspend fun updateStation(id: String, body: OperatorStationRequest): Result<StationDto> = handle { api.operatorUpdateStation(id, body) }
    suspend fun setStatus(id: String, status: String): Result<StationDto> = handle { api.operatorSetStationStatus(id, status) }
    suspend fun addService(stationId: String, body: OperatorServiceRequest): Result<ServiceDto> = handle { api.operatorAddService(stationId, body) }
    suspend fun updateService(stationId: String, serviceId: String, body: OperatorServiceRequest): Result<ServiceDto> =
        handle { api.operatorUpdateService(stationId, serviceId, body) }
    suspend fun deleteService(stationId: String, serviceId: String): Result<Unit> = handle { api.operatorDeleteService(stationId, serviceId) }
    suspend fun stationBookings(): Result<List<BookingDto>> = handle { api.operatorBookings() }

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
            403 -> serverMessage ?: "Operators only — you can only manage your own stations"
            else -> serverMessage ?: "Request failed (HTTP ${response.code()})"
        }
    }
}
