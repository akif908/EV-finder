package com.example.evfinder.feature.news

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.evfinder.ui.components.EvPrimaryButton
import com.example.evfinder.ui.components.EvTopBar
import com.example.evfinder.ui.theme.EvColors

/**
 * Full article view: title, category chip, published time, source, image
 * (if any), body and a "Read Original Source" button when a URL exists.
 */
@Composable
fun NewsDetailScreen(
    newsId: String,
    onBack: () -> Unit
) {
    val viewModel: NewsDetailViewModel = viewModel(
        key = newsId,
        factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                NewsDetailViewModel(newsId) as T
        }
    )
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    Column(
        Modifier
            .fillMaxSize()
            .background(EvColors.Background)
    ) {
        EvTopBar(
            title = "News",
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
                EvPrimaryButton("Go back", onClick = onBack, modifier = Modifier.fillMaxWidth(0.5f))
            }

            else -> {
                val news = state.news!!
                Column(
                    Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp)
                ) {
                    Spacer(Modifier.height(4.dp))
                    NewsCategoryChip(news.category)
                    Spacer(Modifier.height(10.dp))
                    Text(
                        news.title,
                        style = MaterialTheme.typography.headlineSmall,
                        color = EvColors.OnBackground,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(10.dp))

                    // ── published time + source rows ──────────────────────
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Schedule, null, tint = EvColors.OnSurfaceVar, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(
                            fullNewsDateTime(news.publishedAt).ifBlank { relativeNewsTime(news.publishedAt) },
                            style = MaterialTheme.typography.bodySmall,
                            color = EvColors.OnSurfaceVar
                        )
                    }
                    if (!news.source.isNullOrBlank()) {
                        Spacer(Modifier.height(6.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Person, null, tint = EvColors.OnSurfaceVar, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(
                                "Source: ${news.source}",
                                style = MaterialTheme.typography.bodySmall,
                                color = EvColors.OnSurfaceVar
                            )
                        }
                    }

                    news.imageUrl?.takeIf { it.isNotBlank() }?.let { url ->
                        Spacer(Modifier.height(14.dp))
                        AsyncImage(
                            model = url,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(190.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(EvColors.SurfaceHigh)
                        )
                    }

                    Spacer(Modifier.height(14.dp))
                    HorizontalDivider(color = EvColors.SurfaceBorder.copy(0.5f))
                    Spacer(Modifier.height(14.dp))

                    Text(
                        news.content,
                        style = MaterialTheme.typography.bodyLarge,
                        color = EvColors.OnSurface,
                        lineHeight = MaterialTheme.typography.bodyLarge.lineHeight * 1.25f
                    )

                    news.sourceUrl?.takeIf { it.isNotBlank() }?.let { url ->
                        Spacer(Modifier.height(20.dp))
                        EvPrimaryButton(
                            text = "Read Original Source",
                            icon = Icons.AutoMirrored.Filled.OpenInNew,
                            onClick = {
                                try {
                                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                                } catch (_: Exception) {
                                    // no browser on device — nothing else to do here
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    Spacer(Modifier.height(28.dp))
                }
            }
        }
    }
}
