package com.example.evfinder.core.network

import com.example.evfinder.core.model.*
import retrofit2.Response
import retrofit2.http.*

/**
 * REST endpoints — MUST stay in sync with the Spring Boot controllers
 * (backend package com.example.EV_finder_api.controller).
 */
interface ApiService {

    // ---- Auth ----
    @POST("api/auth/register")
    suspend fun register(@Body body: RegisterRequest): Response<AuthResponse>

    @POST("api/auth/login")
    suspend fun login(@Body body: LoginRequest): Response<AuthResponse>

    // ---- Station discovery ----
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

    // ---- Vehicles ----
    @GET("api/vehicles/my")
    suspend fun getMyVehicles(): Response<List<VehicleDto>>

    @POST("api/vehicles")
    suspend fun addVehicle(@Body body: AddVehicleRequest): Response<VehicleDto>

    // ---- Availability ----
    @GET("api/services/{serviceId}/slots")
    suspend fun getSlots(
        @Path("serviceId") serviceId: String,
        @Query("date") date: String
    ): Response<List<SlotDto>>

    // ---- Bookings ----
    @POST("api/bookings")
    suspend fun createBooking(@Body body: BookingRequestDto): Response<BookingDto>

    @GET("api/bookings/my")
    suspend fun getMyBookings(@Query("status") status: String? = null): Response<List<BookingDto>>

    @PUT("api/bookings/{id}/cancel")
    suspend fun cancelBooking(@Path("id") id: String): Response<BookingDto>

    // ---- Simulated payment ----
    @POST("api/payments/{bookingId}")
    suspend fun pay(@Path("bookingId") bookingId: String, @Body body: PaymentRequestDto): Response<PaymentDto>
}
