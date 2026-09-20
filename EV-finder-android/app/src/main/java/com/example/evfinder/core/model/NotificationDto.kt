package com.example.evfinder.core.model

data class NotificationDto(
    val id: String,
    val title: String,
    val message: String,
    val type: String,
    val read: Boolean,
    val createdAt: String?
)

data class UnreadCountDto(val count: Long)

data class UserMeDto(
    val id: String,
    val name: String,
    val email: String,
    val phone: String?,
    val role: String,
    val createdAt: String?
)

data class UpdateProfileRequest(val name: String, val phone: String?)

data class ChangePasswordRequest(val oldPassword: String, val newPassword: String)

data class ReviewRequestDto(
    val bookingId: String,
    val rating: Int,
    val comment: String?
)

data class StationReviewsDto(
    val averageRating: Double,
    val count: Int,
    val reviews: List<ReviewItemDto>
)

data class ReviewItemDto(
    val id: String,
    val userName: String,
    val rating: Int,
    val comment: String?,
    val createdAt: String?
)
