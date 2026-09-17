package com.example.evfinder.feature.station

import com.example.evfinder.core.model.ReviewDto
import com.example.evfinder.core.model.ReviewRequest
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

    suspend fun getReviews(stationId: String): Result<List<ReviewDto>> =
        handle { api.getReviews(stationId) }

    /** Backend requires a COMPLETED booking at the station; 403 message surfaces otherwise. */
    suspend fun postReview(stationId: String, rating: Int, comment: String?): Result<ReviewDto> =
        handle { api.postReview(stationId, ReviewRequest(rating, comment?.trim()?.takeIf { it.isNotBlank() })) }

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
