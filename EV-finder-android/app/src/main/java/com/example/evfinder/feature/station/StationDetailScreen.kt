package com.example.evfinder.feature.station

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Cable
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.evfinder.core.model.ServiceDto
import com.example.evfinder.core.model.StationDto
import com.example.evfinder.core.model.StationReviewsDto
import com.example.evfinder.ui.components.EvCard
import com.example.evfinder.ui.components.EvIconTile
import com.example.evfinder.ui.components.EvSpecTile
import com.example.evfinder.ui.components.EvStatusChip
import com.example.evfinder.ui.components.EvWideButton
import com.example.evfinder.ui.components.EvSectionHeader
import com.example.evfinder.ui.components.StatusPill
import com.example.evfinder.ui.theme.EvColors

/**
 * Station details — mockup language: back header with rating, info card,
 * spec-bento per service with Book CTA, and the review list.
 */
@Composable
fun StationDetailScreen(
    stationId: String,
    onBack: () -> Unit,
    onBookService: (stationId: String, serviceId: String) -> Unit,
    onGetDirections: (latitude: Double, longitude: Double, name: String) -> Unit
) {
    var station by remember { mutableStateOf<StationDto?>(null) }
    var reviews by remember { mutableStateOf<StationReviewsDto?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(true) }

    LaunchedEffect(stationId) {
        StationRepository().getStation(stationId).fold(
            onSuccess = { station = it },
            onFailure = { error = it.message }
        )
        StationRepository().stationReviews(stationId).fold(
            onSuccess = { reviews = it },
            onFailure = { /* reviews optional */ }
        )
        loading = false
    }

    when {
        loading -> Box(Modifier.fillMaxSize().background(EvColors.Background),
            contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = EvColors.Primary)
        }

        error != null -> Box(Modifier.fillMaxSize().background(EvColors.Background),
            contentAlignment = Alignment.Center) {
            Text(error!!, color = EvColors.Error)
        }

        station != null -> StationDetailContent(
            station!!, reviews, onBack, onBookService, onGetDirections
        )
    }
}

@Composable
private fun StationDetailContent(
    station: StationDto,
    reviews: StationReviewsDto?,
    onBack: () -> Unit,
    onBookService: (String, String) -> Unit,
    onGetDirections: (Double, Double, String) -> Unit
) {
    LazyColumn(
        Modifier.fillMaxSize().background(EvColors.Background),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // ── header ──
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(11.dp))
                        .background(EvColors.Surface)
                        .clickable(onClick = onBack),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back",
                        tint = EvColors.OnSurface, modifier = Modifier.size(20.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(station.name, style = MaterialTheme.typography.titleLarge,
                        color = EvColors.OnBackground, fontWeight = FontWeight.Bold,
                        maxLines = 1)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (reviews != null && reviews.count > 0) {
                            Icon(Icons.Filled.Star, null, tint = EvColors.Primary,
                                modifier = Modifier.size(13.dp))
                            Spacer(Modifier.width(3.dp))
                            Text(
                                "${reviews.averageRating} (${reviews.count})",
                                style = MaterialTheme.typography.labelMedium,
                                color = EvColors.Primary, fontWeight = FontWeight.SemiBold
                            )
                        } else {
                            Text("Not rated yet", style = MaterialTheme.typography.labelMedium,
                                color = EvColors.OnSurfaceVar)
                        }
                        Spacer(Modifier.width(8.dp))
                        StatusPill(
                            label = if (station.status == "ACTIVE") "Open"
                            else station.status.lowercase().replace('_', ' '),
                            isActive = station.status == "ACTIVE"
                        )
                    }
                }
            }
        }

        // ── info card ──
        item {
            EvCard(Modifier.fillMaxWidth(), color = EvColors.Surface) {
                Column(Modifier.fillMaxWidth().padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.LocationOn, null, tint = EvColors.Primary,
                            modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(
                            station.address ?: "Address unavailable",
                            style = MaterialTheme.typography.bodySmall,
                            color = EvColors.OnSurfaceVar,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    if (station.openingTime != null && station.closingTime != null) {
                        Spacer(Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Schedule, null, tint = EvColors.OnSurfaceVar,
                                modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(
                                "Open ${station.openingTime?.take(5)} – ${station.closingTime?.take(5)} daily",
                                style = MaterialTheme.typography.labelMedium,
                                color = EvColors.OnSurface
                            )
                        }
                    }
                    station.description?.let {
                        Spacer(Modifier.height(8.dp))
                        Text(it, style = MaterialTheme.typography.bodySmall,
                            color = EvColors.OnSurface)
                    }
                    Spacer(Modifier.height(14.dp))
                    EvWideButton(
                        text = "Get Directions",
                        icon = Icons.Filled.NearMe,
                        container = EvColors.SurfaceHigh,
                        contentColor = EvColors.OnSurface,
                        onClick = { onGetDirections(station.latitude, station.longitude, station.name) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        // ── services ──
        item {
            EvSectionHeader("Services", icon = Icons.Filled.Bolt)
        }
        if (station.services.isEmpty()) {
            item {
                EvCard(Modifier.fillMaxWidth(), color = EvColors.Surface) {
                    Text(
                        "No active services at this station yet.",
                        color = EvColors.OnSurfaceVar,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        }
        items(station.services.size) { index ->
            val service = station.services[index]
            ServiceCard(service, onBook = { onBookService(station.id, service.id) })
        }

        // ── reviews ──
        item {
            EvSectionHeader("Reviews", icon = Icons.Filled.Star, accent = EvColors.Secondary)
        }
        val r = reviews
        if (r == null || r.count == 0) {
            item {
                EvCard(Modifier.fillMaxWidth(), color = EvColors.Surface) {
                    Text(
                        "No reviews yet — book a session and be the first to rate this station.",
                        color = EvColors.OnSurfaceVar,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        } else {
            item {
                // rating summary tile
                EvCard(Modifier.fillMaxWidth(), color = EvColors.Surface) {
                    Row(
                        Modifier.fillMaxWidth().padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                r.averageRating.toBigDecimal().stripTrailingZeros().toPlainString(),
                                style = MaterialTheme.typography.headlineMedium,
                                color = EvColors.Primary, fontWeight = FontWeight.Bold
                            )
                            Text("★".repeat(r.averageRating.toInt().coerceIn(0, 5)),
                                color = EvColors.Primary,
                                style = MaterialTheme.typography.labelSmall)
                        }
                        Spacer(Modifier.width(16.dp))
                        Box(Modifier.width(1.dp).height(36.dp).background(EvColors.SurfaceHighest))
                        Spacer(Modifier.width(16.dp))
                        Text(
                            "${r.count} review${if (r.count == 1) "" else "s"} from verified sessions",
                            style = MaterialTheme.typography.bodySmall,
                            color = EvColors.OnSurfaceVar
                        )
                    }
                }
            }
            items(r.reviews.size) { idx ->
                val review = r.reviews[idx]
                EvCard(Modifier.fillMaxWidth(), color = EvColors.SurfaceLow) {
                    Column(Modifier.fillMaxWidth().padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                Modifier
                                    .size(30.dp)
                                    .clip(CircleShape)
                                    .background(EvColors.SurfaceHighest),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    (review.userName ?: "U").take(1).uppercase(),
                                    color = EvColors.Primary,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text(review.userName ?: "EV user",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = EvColors.OnSurface, fontWeight = FontWeight.SemiBold)
                                Text(review.createdAt?.take(10) ?: "",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = EvColors.OnSurfaceVar)
                            }
                            Text("★".repeat(review.rating) + "☆".repeat(5 - review.rating),
                                color = EvColors.Primary,
                                style = MaterialTheme.typography.labelMedium)
                        }
                        review.comment?.let {
                            Spacer(Modifier.height(8.dp))
                            Text(it, style = MaterialTheme.typography.bodySmall,
                                color = EvColors.OnSurface)
                        }
                    }
                }
            }
        }

        item { Spacer(Modifier.height(8.dp)) }
    }
}

@Composable
private fun ServiceCard(service: ServiceDto, onBook: () -> Unit) {
    val isSwap = service.serviceType == "BATTERY_SWAP"
    val bookable = service.availableSlots > 0

    EvCard(Modifier.fillMaxWidth().animateContentSize(), color = EvColors.Surface) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                EvIconTile(
                    icon = if (isSwap) Icons.Filled.SwapHoriz else Icons.Filled.Bolt,
                    tint = if (isSwap) EvColors.Tertiary else EvColors.Primary,
                    size = 42.dp
                )
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        if (isSwap) "Battery Swap" else "Charging",
                        style = MaterialTheme.typography.titleMedium,
                        color = EvColors.OnBackground, fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        (service.connectorType ?: "") +
                            (service.powerKw?.let { if (isSwap) "" else " · ${it.toInt()} kW" } ?: ""),
                        style = MaterialTheme.typography.labelSmall,
                        color = EvColors.OnSurfaceVar
                    )
                }
                EvStatusChip(
                    label = if (bookable) "${service.availableSlots} free" else "Full",
                    accent = if (bookable) EvColors.Primary else EvColors.Error,
                    pulsingDot = bookable
                )
            }

            Spacer(Modifier.height(14.dp))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (!isSwap && service.powerKw != null) {
                    EvSpecTile(
                        icon = Icons.Filled.Bolt,
                        value = "${service.powerKw.toInt()} kW",
                        caption = "Output",
                        modifier = Modifier.weight(1f)
                    )
                }
                EvSpecTile(
                    icon = Icons.Filled.Cable,
                    value = service.connectorType ?: (if (isSwap) "Pod" else "—"),
                    caption = "Connector",
                    accent = EvColors.Secondary,
                    modifier = Modifier.weight(1f)
                )
                EvSpecTile(
                    icon = Icons.Filled.Check,
                    value = "৳${service.pricePerUnit.toBigDecimal().stripTrailingZeros().toPlainString()}",
                    caption = "Per unit",
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(Modifier.height(14.dp))
            EvWideButton(
                text = if (bookable) "Book this service" else "Fully booked",
                icon = Icons.Filled.Schedule,
                enabled = bookable,
                onClick = onBook,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
