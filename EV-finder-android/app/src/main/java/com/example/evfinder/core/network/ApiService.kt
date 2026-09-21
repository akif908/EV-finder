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

    // ---- Reviews ----
    @GET("api/stations/{stationId}/reviews")
    suspend fun getReviews(@Path("stationId") stationId: String): Response<List<ReviewDto>>

    @POST("api/stations/{stationId}/reviews")
    suspend fun postReview(
        @Path("stationId") stationId: String,
        @Body body: ReviewRequest
    ): Response<ReviewDto>

    // ---- Single service (booking flow cost estimator) ----
    @GET("api/services/{id}")
    suspend fun getService(@Path("id") id: String): Response<ServiceDto>

    // ---- Fuel stations (separate module — read-only for users) ----
    @GET("api/fuel-stations")
    suspend fun getFuelStations(
        @Query("q") query: String? = null,
        @Query("fuel") fuel: String? = null,
        @Query("availableOnly") availableOnly: Boolean = false
    ): Response<List<FuelStationDto>>

    @GET("api/fuel-stations/nearby")
    suspend fun getNearbyFuelStations(
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("radiusKm") radiusKm: Double = 10.0
    ): Response<List<FuelStationDto>>

    @GET("api/fuel-stations/{id}")
    suspend fun getFuelStation(@Path("id") id: String): Response<FuelStationDto>

    // ---- Operator: fuel station management (no booking endpoints exist) ----
    @GET("api/operator/fuel-stations/my")
    suspend fun operatorMyFuelStations(): Response<List<FuelStationDto>>

    @POST("api/operator/fuel-stations")
    suspend fun operatorCreateFuelStation(@Body body: FuelStationRequest): Response<FuelStationDto>

    @PUT("api/operator/fuel-stations/{id}")
    suspend fun operatorUpdateFuelStation(@Path("id") id: String, @Body body: FuelStationRequest): Response<FuelStationDto>

    @DELETE("api/operator/fuel-stations/{id}")
    suspend fun operatorDeleteFuelStation(@Path("id") id: String): Response<Unit>

    @PUT("api/operator/fuel-stations/{id}/open")
    suspend fun operatorSetFuelStationOpen(@Path("id") id: String, @Query("isOpen") isOpen: Boolean): Response<FuelStationDto>

    @PUT("api/operator/fuel-stations/{id}/fuel/{fuelType}")
    suspend fun operatorUpdateFuelInventory(
        @Path("id") id: String,
        @Path("fuelType") fuelType: String,
        @Body body: FuelInventoryUpdateRequest
    ): Response<FuelStationDto>

    @DELETE("api/operator/fuel-stations/{id}/fuel/{fuelType}")
    suspend fun operatorRemoveFuelType(@Path("id") id: String, @Path("fuelType") fuelType: String): Response<Unit>

    // ---- Vehicles ----
    @GET("api/vehicles/my")
    suspend fun getMyVehicles(): Response<List<VehicleDto>>

    @POST("api/vehicles")
    suspend fun addVehicle(@Body body: AddVehicleRequest): Response<VehicleDto>

    @PUT("api/vehicles/{id}")
    suspend fun updateVehicle(@Path("id") id: String, @Body body: AddVehicleRequest): Response<VehicleDto>

    @DELETE("api/vehicles/{id}")
    suspend fun deleteVehicle(@Path("id") id: String): Response<Unit>

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

    // ---- Operator: station & service management ----
    @GET("api/operator/stations/my")
    suspend fun operatorMyStations(): Response<List<StationDto>>

    @POST("api/operator/stations")
    suspend fun operatorCreateStation(@Body body: OperatorStationRequest): Response<StationDto>

    @PUT("api/operator/stations/{id}")
    suspend fun operatorUpdateStation(@Path("id") id: String, @Body body: OperatorStationRequest): Response<StationDto>

    @PUT("api/operator/stations/{id}/status")
    suspend fun operatorSetStationStatus(@Path("id") id: String, @Query("status") status: String): Response<StationDto>

    @POST("api/operator/stations/{id}/services")
    suspend fun operatorAddService(@Path("id") id: String, @Body body: OperatorServiceRequest): Response<ServiceDto>

    @PUT("api/operator/stations/{stationId}/services/{serviceId}")
    suspend fun operatorUpdateService(
        @Path("stationId") stationId: String,
        @Path("serviceId") serviceId: String,
        @Body body: OperatorServiceRequest
    ): Response<ServiceDto>

    @DELETE("api/operator/stations/{stationId}/services/{serviceId}")
    suspend fun operatorDeleteService(
        @Path("stationId") stationId: String,
        @Path("serviceId") serviceId: String
    ): Response<Unit>

    // ---- Operator: bookings at owned stations ----
    @GET("api/operator/bookings")
    suspend fun operatorBookings(): Response<List<BookingDto>>

    // ---- Energy & fuel news (nation-wide feed; admin manages content) ----
    @GET("api/news")
    suspend fun getNews(
        @Query("category") category: String? = null,
        @Query("includeUnpublished") includeUnpublished: Boolean = false
    ): Response<List<NewsDto>>

    @GET("api/news/{id}")
    suspend fun getNewsItem(@Path("id") id: String): Response<NewsDto>

    @POST("api/news")
    suspend fun createNews(@Body body: NewsRequestDto): Response<NewsDto>

    @PUT("api/news/{id}")
    suspend fun updateNews(@Path("id") id: String, @Body body: NewsRequestDto): Response<NewsDto>

    @PUT("api/news/{id}/publish")
    suspend fun setNewsPublished(@Path("id") id: String, @Query("published") published: Boolean): Response<NewsDto>

    @DELETE("api/news/{id}")
    suspend fun deleteNews(@Path("id") id: String): Response<Unit>
}
