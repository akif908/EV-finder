package com.example.evfinder.core.model

/** Mirrors the backend NewsResponse JSON — one energy/fuel/EV headline. */
data class NewsArticleDto(
    val id: String,
    val title: String,
    val link: String,
    val source: String? = null,
    /** EV | LPG | FUEL | POLICY */
    val category: String = "GENERAL",
    val summary: String? = null,
    val publishedAt: String? = null
)

/** Categories shown as filters in the news section. */
object NewsCategories {
    /** value passed to the API (null = all) to label shown in the UI. */
    val ALL = listOf(
        null to "All",
        "FUEL" to "Fuel",
        "LPG" to "LPG",
        "EV" to "EV",
        "POLICY" to "Policy"
    )
}
