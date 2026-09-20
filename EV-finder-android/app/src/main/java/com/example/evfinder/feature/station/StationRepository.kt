package com.example.evfinder.feature.station

import com.example.evfinder.core.model.StationDto
import com.example.evfinder.core.network.ApiClient
import com.example.evfinder.core.network.ApiService
import retrofit2.Response
import java.io.IOException

/** Loads stations from the backend discovery endpoints. */
class StationRepository {

    private val api: ApiService = ApiClient.retrofit.create(ApiService::class.java)

    suspend fun getStations(query: String? = null): Result<List<StationDto>> =
        handle { api.getStations(query?.takeIf { it.isNotBlank() }) }

    suspend fun getNearby(latitude: Double, longitude: Double, radiusKm: Double = 10.0): Result<List<StationDto>> =
        handle { api.getNearbyStations(latitude, longitude, radiusKm) }

    suspend fun getStation(id: String): Result<StationDto> =
        handle { api.getStation(id) }

    /** Average rating + reviews for a station (reviews controller). */
    suspend fun stationReviews(stationId: String): Result<com.example.evfinder.core.model.StationReviewsDto> =
        handle { api.stationReviews(stationId) }

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

    /** Human-readable error incl. the backend's own message (see GlobalExceptionHandler). */
    private fun <T> errorMessage(response: Response<T>): String {
        val serverMessage = try {
            org.json.JSONObject(response.errorBody()?.string() ?: "{}")
                .optString("message", "").takeIf { it.isNotBlank() }
        } catch (e: Exception) { null }

        return when (response.code()) {
            401 -> serverMessage ?: "Session expired — please log in again"
            403 -> serverMessage ?: "You don't have access to this"
            else -> serverMessage ?: "Request failed (HTTP ${response.code()})"
        }
    }
}
