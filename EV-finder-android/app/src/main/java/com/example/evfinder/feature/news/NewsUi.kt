package com.example.evfinder.feature.news

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Article
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.evfinder.core.model.NewsDto
import com.example.evfinder.ui.components.EvCard
import com.example.evfinder.ui.components.StatusPill
import com.example.evfinder.ui.theme.EvColors
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ─── Category metadata (same five categories for every user) ──────────────────

data class NewsCategoryMeta(
    val key: String,
    val emoji: String,
    val label: String,
    val icon: ImageVector,
    val tint: Color
)

/** Category order used by filter rows and the admin category picker. */
val allNewsCategories = listOf("GAS_CNG", "FUEL_PRICE", "EV_CHARGING", "TRANSPORT_ENERGY", "GOVERNMENT")

fun newsCategoryMeta(category: String?): NewsCategoryMeta = when (category) {
    "GAS_CNG" -> NewsCategoryMeta("GAS_CNG", "🔴", "Gas / CNG",
        Icons.Filled.LocalFireDepartment, EvColors.Error)
    "FUEL_PRICE" -> NewsCategoryMeta("FUEL_PRICE", "⛽", "Fuel Price",
        Icons.Filled.LocalGasStation, EvColors.Warning)
    "EV_CHARGING" -> NewsCategoryMeta("EV_CHARGING", "⚡", "EV & Charging",
        Icons.Filled.Bolt, EvColors.Primary)
    "TRANSPORT_ENERGY" -> NewsCategoryMeta("TRANSPORT_ENERGY", "🚗", "Transport & Energy",
        Icons.Filled.DirectionsCar, EvColors.Accent)
    "GOVERNMENT" -> NewsCategoryMeta("GOVERNMENT", "🏛", "Government / Energy",
        Icons.Filled.AccountBalance, Color(0xFF6C9FFF))
    else -> NewsCategoryMeta(category ?: "GENERAL", "📰", "Energy News",
        Icons.Filled.Article, EvColors.OnSurfaceVar)
}

// ─── Relative time labels ("2 hours ago", "Yesterday") ────────────────────────
// Backend sends ISO local datetimes ("2026-09-21T14:30:00"); SimpleDateFormat
// keeps this working on every API level without java.time desugaring.

private fun parseIsoMillis(iso: String?): Long? {
    if (iso.isNullOrBlank()) return null
    val formats = listOf("yyyy-MM-dd'T'HH:mm:ss", "yyyy-MM-dd'T'HH:mm")
    for (pattern in formats) {
        try {
            return SimpleDateFormat(pattern, Locale.US).parse(iso)?.time ?: continue
        } catch (_: Exception) { /* try next pattern */ }
    }
    return null
}

fun relativeNewsTime(iso: String?): String {
    val time = parseIsoMillis(iso) ?: return ""
    val minutes = (System.currentTimeMillis() - time) / 60_000
    val hours = minutes / 60
    val days = hours / 24
    return when {
        minutes < 0 -> "Scheduled"                       // future publication date
        minutes < 1 -> "Just now"
        hours < 1 -> "$minutes minute${if (minutes == 1L) "" else "s"} ago"
        hours < 24 -> "$hours hour${if (hours == 1L) "" else "s"} ago"
        days == 1L -> "Yesterday"
        days < 7 -> "$days days ago"
        else -> SimpleDateFormat("MMM d, yyyy", Locale.US).format(Date(time))
    }
}

/** Full timestamp for the detail screen, e.g. "Sep 21, 2026 · 2:30 PM". */
fun fullNewsDateTime(iso: String?): String {
    val time = parseIsoMillis(iso) ?: return ""
    return SimpleDateFormat("MMM d, yyyy · h:mm a", Locale.US).format(Date(time))
}

/** ISO string for right now (admin form default publication date). */
fun nowIsoForNews(): String =
    SimpleDateFormat("yyyy-MM-dd'T'HH:mm", Locale.US).format(Date())

// ─── News card (Home section + View All list) ─────────────────────────────────

/**
 * Rounded news card: category emoji chip + title, short description, and a
 * footer with the relative published time and a "Read More" action.
 * The whole card is clickable.
 */
@Composable
fun NewsCard(
    news: NewsDto,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    showReadMore: Boolean = true
) {
    val meta = newsCategoryMeta(news.category)
    EvCard(modifier = modifier.fillMaxWidth(), onClick = onClick) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(meta.tint.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(meta.emoji, color = Color.Unspecified)
                }
                Spacer(Modifier.width(10.dp))
                Text(
                    news.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = EvColors.OnBackground,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                news.shortDescription,
                style = MaterialTheme.typography.bodySmall,
                color = EvColors.OnSurfaceVar,
                maxLines = 3
            )
            Spacer(Modifier.height(10.dp))
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    relativeNewsTime(news.publishedAt),
                    style = MaterialTheme.typography.labelMedium,
                    color = EvColors.OnSurfaceVar,
                    modifier = Modifier.weight(1f)
                )
                if (showReadMore) {
                    Text(
                        "Read More",
                        style = MaterialTheme.typography.labelMedium,
                        color = EvColors.Primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                if (!news.isPublished) {
                    Spacer(Modifier.width(8.dp))
                    StatusPill(label = "Draft", isActive = false)
                }
            }
        }
    }
}

/** Small emoji + label chip used on filters and the detail screen. */
@Composable
fun NewsCategoryChip(category: String?, modifier: Modifier = Modifier) {
    val meta = newsCategoryMeta(category)
    Row(
        modifier
            .clip(RoundedCornerShape(50))
            .background(meta.tint.copy(alpha = 0.14f))
            .border(1.dp, meta.tint.copy(alpha = 0.4f), RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Icon(meta.icon, null, tint = meta.tint, modifier = Modifier.size(13.dp))
        Text(
            meta.label,
            style = MaterialTheme.typography.labelSmall,
            color = meta.tint,
            fontWeight = FontWeight.SemiBold
        )
    }
}
