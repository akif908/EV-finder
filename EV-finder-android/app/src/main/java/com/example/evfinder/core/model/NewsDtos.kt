package com.example.evfinder.core.model

/**
 * Energy & Fuel News module — Bangladesh-focused, nation-wide feed
 * (same items for every user, no location filtering).
 * Mirrors backend NewsResponse JSON (see NewsResponse.java).
 */
data class NewsDto(
    val id: String,
    val title: String,
    val shortDescription: String,
    val content: String = "",         // required on the backend (@NotBlank)
    val category: String,             // GAS_CNG | FUEL_PRICE | EV_CHARGING | TRANSPORT_ENERGY | GOVERNMENT
    val source: String? = null,
    val sourceUrl: String? = null,
    val imageUrl: String? = null,
    val publishedAt: String? = null,  // ISO local datetime, e.g. 2026-09-21T14:30:00
    val isPublished: Boolean = true,
    val createdAt: String? = null,
    val updatedAt: String? = null
)

/** Mirrors backend NewsRequest (admin create/update). */
data class NewsRequestDto(
    val title: String,
    val shortDescription: String,
    val content: String,
    val category: String,
    val source: String?,
    val sourceUrl: String?,
    val imageUrl: String?,
    val publishedAt: String,          // ISO local datetime accepted by the backend
    val isPublished: Boolean
)
