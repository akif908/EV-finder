package com.example.evfinder.core.model

/** Mirrors the backend IssueResponse JSON. */
data class IssueDto(
    val id: String,
    val userId: String,
    val userName: String? = null,
    val userEmail: String? = null,
    val stationId: String? = null,
    val stationName: String? = null,
    val category: String,
    val subject: String,
    val description: String,
    /** OPEN | IN_PROGRESS | RESOLVED | REJECTED */
    val status: String,
    val resolutionNote: String? = null,
    val createdAt: String? = null,
    val resolvedAt: String? = null
)

data class IssueRequestDto(
    val category: String,
    val subject: String,
    val description: String,
    val stationId: String? = null
)

data class IssueUpdateDto(
    val status: String,
    val resolutionNote: String? = null
)

/** Report categories offered in the app. */
object IssueCategories {
    val ALL = listOf(
        "Bug / crash",
        "Charging problem",
        "Wrong station info",
        "Payment issue",
        "Booking problem",
        "Account",
        "Other"
    )
}
