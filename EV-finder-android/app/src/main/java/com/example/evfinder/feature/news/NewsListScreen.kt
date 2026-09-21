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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.evfinder.ui.components.EvPrimaryButton
import com.example.evfinder.ui.components.EvTopBar
import com.example.evfinder.ui.theme.EvColors

/**
 * View All News: every published item with category filter chips and a
 * search field. Same nation-wide list for every user.
 */
@Composable
fun NewsListScreen(
    onBack: () -> Unit,
    onNewsClick: (String) -> Unit,
    viewModel: NewsListViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()

    Column(
        Modifier
            .fillMaxSize()
            .background(EvColors.Background)
    ) {
        EvTopBar(
            title = "Energy & Fuel News",
            subtitle = "Bangladesh energy updates",
            navigationContent = {
                Box(
                    Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(EvColors.SurfaceHigh)
                        .border(1.dp, EvColors.SurfaceBorder, RoundedCornerShape(10.dp))
                        .clickable(onClick = onBack),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back",
                        tint = EvColors.OnSurface, modifier = Modifier.size(18.dp))
                }
            }
        )

        // ── search field ──────────────────────────────────────────────────
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(EvColors.SurfaceHigh)
                .border(1.dp, EvColors.SurfaceBorder, RoundedCornerShape(14.dp))
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Outlined.Search, null, tint = EvColors.OnSurfaceVar, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(10.dp))
            BasicNewsSearchField(
                value = state.query,
                onValueChange = viewModel::onQueryChanged,
                placeholder = "Search news",
                modifier = Modifier.weight(1f)
            )
        }

        // ── category filter chips ─────────────────────────────────────────
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item { NewsFilterPill("All", null, state.category, viewModel::setCategory) }
            items(allNewsCategories, key = { it }) { key ->
                NewsFilterPill(newsCategoryMeta(key).label, key, state.category, viewModel::setCategory)
            }
        }

        when {
            state.loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = EvColors.Primary)
            }

            state.error != null -> Column(
                Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(Icons.Filled.WifiOff, null, tint = EvColors.OnSurfaceVar, modifier = Modifier.size(44.dp))
                Spacer(Modifier.height(12.dp))
                Text(state.error!!, color = EvColors.Error, style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(12.dp))
                TextButton(onClick = viewModel::load) { Text("Retry", color = EvColors.Primary) }
            }

            else -> {
                val visible = state.visibleNews
                LazyColumn(
                    Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (visible.isEmpty()) {
                        item {
                            Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Filled.SearchOff, null,
                                        tint = EvColors.OnSurfaceVar, modifier = Modifier.size(40.dp))
                                    Spacer(Modifier.height(12.dp))
                                    Text(
                                        if (state.query.isBlank()) "No news in this category yet"
                                        else "No news match \"${state.query}\"",
                                        color = EvColors.OnSurfaceVar
                                    )
                                }
                            }
                        }
                    } else {
                        items(visible, key = { it.id }) { news ->
                            NewsCard(news = news, onClick = { onNewsClick(news.id) })
                        }
                        item { Spacer(Modifier.height(8.dp)) }
                    }
                }
            }
        }
    }
}

@Composable
private fun NewsFilterPill(
    label: String,
    category: String?,
    selected: String?,
    onSelect: (String?) -> Unit
) {
    val active = selected == category
    Box(
        Modifier
            .clip(RoundedCornerShape(50))
            .background(if (active) EvColors.Primary else EvColors.SurfaceHigh)
            .border(1.dp, if (active) EvColors.Primary else EvColors.SurfaceBorder, RoundedCornerShape(50))
            .clickable { onSelect(category) }
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Text(
            label,
            color = if (active) EvColors.OnPrimary else EvColors.OnSurface,
            style = MaterialTheme.typography.labelMedium,
            maxLines = 1
        )
    }
}

@Composable
internal fun BasicNewsSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier
) {
    androidx.compose.foundation.text.BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        textStyle = MaterialTheme.typography.bodyMedium.copy(color = EvColors.OnBackground),
        decorationBox = { inner ->
            if (value.isEmpty()) Text(placeholder, style = MaterialTheme.typography.bodyMedium, color = EvColors.OnSurfaceVar)
            inner()
        },
        singleLine = true,
        cursorBrush = androidx.compose.ui.graphics.SolidColor(EvColors.Primary)
    )
}
