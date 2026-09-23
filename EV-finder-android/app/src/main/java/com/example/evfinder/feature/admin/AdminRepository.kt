package com.example.evfinder.feature.admin

import com.example.evfinder.core.model.AdminOverviewDto
import com.example.evfinder.core.model.AdminUserDto
import com.example.evfinder.core.model.BookingDto
import com.example.evfinder.core.network.ApiClient
import com.example.evfinder.core.network.ApiService
import org.json.JSONObject
import retrofit2.Response
import java.io.IOException

class AdminRepository {
    private val api: ApiService = ApiClient.retrofit.create(ApiService::class.java)

    suspend fun overview(): Result<AdminOverviewDto> = handle { api.adminOverview() }
    suspend fun users(role: String? = null): Result<List<AdminUserDto>> = handle { api.adminUsers(role) }
    suspend fun changeRole(userId: String, role: String): Result<AdminUserDto> = handle { api.adminChangeRole(userId, role) }
    suspend fun deleteUser(userId: String): Result<Unit> = handle { api.adminDeleteUser(userId) }
    suspend fun bookings(): Result<List<BookingDto>> = handle { api.adminBookings() }
    suspend fun forceCancelBooking(id: String): Result<BookingDto> = handle { api.adminForceCancelBooking(id) }

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
            403 -> serverMessage ?: "Admins only"
            else -> serverMessage ?: "Request failed (HTTP ${response.code()})"
        }
    }

    // ---- issue reports ----
    suspend fun issues(): Result<List<com.example.evfinder.core.model.IssueDto>> =
        com.example.evfinder.feature.support.IssueRepository().all()

    suspend fun updateIssue(id: String, status: String, note: String?):
        Result<com.example.evfinder.core.model.IssueDto> =
        com.example.evfinder.feature.support.IssueRepository().update(id, status, note)
}
