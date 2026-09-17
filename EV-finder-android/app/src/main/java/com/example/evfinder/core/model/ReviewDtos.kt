package com.example.evfinder.core.model

/** Mirrors the backend ReviewResponse JSON (see ReviewResponse.java). */
data class ReviewDto(
    val id: String,
    val userId: String,
    val userName: String?,
    val rating: Int,
    val comment: String?,
    val createdAt: String?
)

/** Mirrors backend ReviewRequest (create/update own review). */
data class ReviewRequest(
    val rating: Int,        // 1–5
    val comment: String?
)
