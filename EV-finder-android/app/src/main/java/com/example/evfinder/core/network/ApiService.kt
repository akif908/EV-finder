package com.example.evfinder.core.network

import com.example.evfinder.core.model.AuthResponse
import com.example.evfinder.core.model.LoginRequest
import com.example.evfinder.core.model.RegisterRequest
import com.example.evfinder.core.model.StationDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * REST endpoints — MUST stay in sync with the Spring Boot controllers
 * (backend package com.example.EV_finder_api.controller).
 */
interface ApiService {

    @POST("api/auth/register")
    suspend fun register(@Body body: RegisterRequest): Response<AuthResponse>

    @POST("api/auth/login")
    suspend fun login(@Body body: LoginRequest): Response<AuthResponse>

    // ---- Station discovery (StationController) ----

    @GET("api/stations")
    suspend fun getStations(@Query("q") query: String? = null): Response<List<StationDto>>

    @GET("api/stations/nearby")
    suspend fun getNearbyStations(
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("radiusKm") radiusKm: Double = 10.0
    ): Response<List<StationDto>>

    @GET("api/stations/{id}")
    suspend fun getStation(@Path("id") id: String): Response<StationDto>
}
