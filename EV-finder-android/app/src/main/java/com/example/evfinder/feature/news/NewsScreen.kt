package com.example.evfinder.feature.news

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Newspaper
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.evfinder.core.model.NewsArticleDto
import com.example.evfinder.core.model.NewsCategories
import com.example.evfinder.ui.components.EvFilterChip
import com.example.evfinder.ui.components.EvTopBar
import com.example.evfinder.ui.theme.EvColors
import java.time.Duration
import java.time.LocalDateTime

/** Accent per category so the feed is scannable at a glance. */
private fun categoryAccent(category: String) = when (category) {
    "FUEL" -> EvColors.Warning
    "LPG" -> androidx.compose.ui.graphics.Color(0xFF4FC3F7)
    "EV" -> EvColors.Primary
    "POLICY" -> EvColors.Tertiary
    else -> EvColors.OnSurfaceVar
}

private fun categoryLabel(category: String) = when (category) {
    "FUEL" -> "Fuel"
    "LPG" -> "LPG"
    "EV" -> "EV"
    "POLICY" -> "Policy"
    else -> "News"
}

/** "2h ago" style stamp, falling back to the date when unparseable. */
fun relativeTime(iso: String?): String {
    if (iso.isNullOrBlank()) return ""
    return runCatching {
        val minutes = Duration.between(LocalDateTime.parse(iso), LocalDateTime.now()).toMinutes()
        when {
            minutes < 1 -> "just now"
            minutes < 60 -> "${minutes}m ago"
            minutes < 60 * 24 -> "${minutes / 60}h ago"
            else -> "${minutes / (60 * 24)}d ago"
        }
    }.getOrDefault(iso.take(10))
}

/**
 * "Latest energy & fuel updates" — the user-side news section. Headlines are
 * real, pulled from free public RSS (fuel, LPG, EV and energy policy).
 */
@Composable
fun EnergyUpdatesSection(
    onOpenArticle: (String) -> Unit,
    onSeeAll: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: NewsViewModel = viewModel(factory = newsPreviewFactory())
) {
    val state by viewModel.uiState.collectAsState()

    Column(modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Filled.Newspaper, null, tint = EvColors.Secondary, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(
                "Energy & fuel updates",
                style = MaterialTheme.typography.titleMedium,
                color = EvColors.OnBackground,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f)
            )
            if (state.loading) {
                CircularProgressIndicator(Modifier.size(14.dp), strokeWidth = 2.dp, color = EvColors.Primary)
            } else if (state.articles.isNotEmpty()) {
                Text(
                    "See all",
                    color = EvColors.Primary,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(onClick = onSeeAll)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }

        when {
            state.error != null && state.articles.isEmpty() -> Text(
                state.error!!,
                style = MaterialTheme.typography.bodySmall,
                color = EvColors.OnSurfaceVar,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            state.articles.isEmpty() && !state.loading -> Text(
                "No updates yet — headlines appear here as soon as they are published.",
                style = MaterialTheme.typography.bodySmall,
                color = EvColors.OnSurfaceVar,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            else -> LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(state.articles.take(8), key = { it.id }) { article ->
                    NewsCard(article, onClick = { onOpenArticle(article.link) }, compact = true)
                }
            }
        }
    }
}

/** Compact card used in the Home carousel. */
@Composable
private fun NewsCard(article: NewsArticleDto, onClick: () -> Unit, compact: Boolean) {
    val accent = categoryAccent(article.category)
    Column(
        Modifier
            .then(if (compact) Modifier.width(260.dp) else Modifier.fillMaxWidth())
            .clip(RoundedCornerShape(14.dp))
            .background(EvColors.Surface)
            .border(1.dp, EvColors.SurfaceBorder, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                categoryLabel(article.category).uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = accent,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.7.sp,
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(accent.copy(alpha = 0.12f))
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            )
            Spacer(Modifier.weight(1f))
            if (compact) {
                Icon(
                    Icons.Filled.OpenInNew, null,
                    tint = EvColors.OnSurfaceVar, modifier = Modifier.size(13.dp)
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            article.title,
            style = MaterialTheme.typography.titleSmall,
            color = EvColors.OnBackground,
            fontWeight = FontWeight.SemiBold,
            maxLines = if (compact) 3 else 4
        )
        if (!compact) {
            article.summary?.takeIf { it.isNotBlank() }?.let {
                Spacer(Modifier.height(6.dp))
                Text(it, style = MaterialTheme.typography.bodySmall, color = EvColors.OnSurface, maxLines = 3)
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            listOfNotNull(article.source, relativeTime(article.publishedAt).takeIf { t -> t.isNotBlank() })
                .joinToString(" · "),
            style = MaterialTheme.typography.labelSmall,
            color = EvColors.OnSurfaceVar,
            maxLines = 1
        )
    }
}

/** Full list with category filters — opened from "See all". */
@Composable
fun NewsScreen(onBack: () -> Unit, onOpenArticle: (String) -> Unit) {
    val vm: NewsViewModel = viewModel()
    val state by vm.uiState.collectAsState()

    Column(Modifier.fillMaxSize().background(EvColors.Background)) {
        EvTopBar(
            title = "Energy & fuel updates",
            subtitle = "Fuel, LPG, EV and policy headlines",
            navigationContent = {
                Box(
                    Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(11.dp))
                        .background(EvColors.Surface)
                        .clickable(onClick = onBack),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.ArrowBack, "Back", tint = EvColors.OnSurface, modifier = Modifier.size(19.dp))
                }
            }
        )

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(NewsCategories.ALL) { (value, label) ->
                EvFilterChip(label, state.category == value, { vm.setCategory(value) })
            }
        }
        Spacer(Modifier.height(12.dp))

        when {
            state.loading && state.articles.isEmpty() -> Box(
                Modifier.fillMaxSize(), contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = EvColors.Primary)
            }

            state.error != null && state.articles.isEmpty() -> Column(
                Modifier.fillMaxSize().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(Icons.Filled.WifiOff, null, tint = EvColors.OnSurfaceVar, modifier = Modifier.size(40.dp))
                Spacer(Modifier.height(12.dp))
                Text(state.error!!, color = EvColors.Error, style = MaterialTheme.typography.bodyMedium)
                TextButton(onClick = vm::load) { Text("Retry", color = EvColors.Primary) }
            }

            state.articles.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No headlines in this category yet.", color = EvColors.OnSurfaceVar)
            }

            else -> LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(state.articles, key = { it.id }) { article ->
                    NewsCard(article, onClick = { onOpenArticle(article.link) }, compact = false)
                }
            }
        }
    }
}

/** Home's carousel asks for a short list; the full screen asks for everything. */
private fun newsPreviewFactory() = viewModelFactory {
    initializer { NewsViewModel(previewLimit = 8) }
}
