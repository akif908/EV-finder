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

    @GET("api/bookings/{id}")
    suspend fun getBooking(@Path("id") id: String): Response<BookingDto>

    @PUT("api/bookings/{id}/cancel")
    suspend fun cancelBooking(@Path("id") id: String): Response<BookingDto>

    // ---- Simulated payment ----
    @POST("api/payments/{bookingId}")
    suspend fun pay(@Path("bookingId") bookingId: String, @Body body: PaymentRequestDto): Response<PaymentDto>

    // ---- Profile self-service (all roles) ----
    @GET("api/users/me")
    suspend fun getMe(): Response<UserMeDto>

    @PUT("api/users/me")
    suspend fun updateMe(@Body body: UpdateProfileRequest): Response<UserMeDto>

    @PUT("api/users/me/password")
    suspend fun changePassword(@Body body: ChangePasswordRequest): Response<Unit>

    // ---- Notifications ----
    @GET("api/notifications/my")
    suspend fun myNotifications(): Response<List<NotificationDto>>

    @GET("api/notifications/unread-count")
    suspend fun unreadCount(): Response<UnreadCountDto>

    @PUT("api/notifications/{id}/read")
    suspend fun markNotificationRead(@Path("id") id: String): Response<NotificationDto>

    @PUT("api/notifications/read-all")
    suspend fun markAllNotificationsRead(): Response<Unit>

    // ---- Reviews ----
    @POST("api/reviews")
    suspend fun createReview(@Body body: ReviewRequestDto): Response<ReviewItemDto>

    @GET("api/reviews/station/{stationId}")
    suspend fun stationReviews(@Path("stationId") stationId: String): Response<StationReviewsDto>

    // ---- Operator ----
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

    @GET("api/operator/bookings")
    suspend fun operatorBookings(): Response<List<BookingDto>>

    // ---- Admin ----
    @GET("api/admin/overview")
    suspend fun adminOverview(): Response<AdminOverviewDto>

    @GET("api/admin/users")
    suspend fun adminUsers(@Query("role") role: String? = null): Response<List<AdminUserDto>>

    @PUT("api/admin/users/{id}/role")
    suspend fun adminChangeRole(@Path("id") id: String, @Query("role") role: String): Response<AdminUserDto>

    @DELETE("api/admin/users/{id}")
    suspend fun adminDeleteUser(@Path("id") id: String): Response<Unit>

    @GET("api/admin/bookings")
    suspend fun adminBookings(): Response<List<BookingDto>>

    @PUT("api/admin/bookings/{id}/cancel")
    suspend fun adminForceCancelBooking(@Path("id") id: String): Response<BookingDto>
}
