package com.example.evfinder.feature.auth

import com.example.evfinder.core.model.AuthResponse
import com.example.evfinder.core.model.LoginRequest
import com.example.evfinder.core.model.RegisterRequest
import com.example.evfinder.core.network.ApiClient
import com.example.evfinder.core.network.ApiService
import org.json.JSONObject
import retrofit2.Response
import java.io.IOException

/**
 * Calls the Spring Boot /api/auth endpoints and converts HTTP failures
 * into readable messages (the backend returns {message: "..."} via
 * GlobalExceptionHandler).
 */
class AuthRepository {

    private val api: ApiService = ApiClient.retrofit.create(ApiService::class.java)

    suspend fun register(name: String, email: String, password: String, role: String): Result<AuthResponse> =
        handle { api.register(RegisterRequest(name, email, password, role)) }

    suspend fun login(email: String, password: String): Result<AuthResponse> =
        handle { api.login(LoginRequest(email, password)) }

    private suspend fun handle(call: suspend () -> Response<AuthResponse>): Result<AuthResponse> {
        return try {
            val response = call()
            val body = response.body()
            if (response.isSuccessful && body != null) {
                Result.success(body)
            } else {
                Result.failure(Exception(parseError(response)))
            }
        } catch (e: IOException) {
            Result.failure(Exception("Cannot reach the server. Is the backend running?"))
        }
    }

    private fun parseError(response: Response<AuthResponse>): String {
        return try {
            val json = JSONObject(response.errorBody()?.string() ?: "{}")
            json.optString("message", "Request failed (${response.code()})")
        } catch (e: Exception) {
            "Request failed (${response.code()})"
        }
    }
}
