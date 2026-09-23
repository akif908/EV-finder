package com.example.evfinder.feature.station

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.evfinder.core.model.StationDto
import com.example.evfinder.core.model.StationReviewsDto
import com.example.evfinder.ui.components.*
import com.example.evfinder.ui.theme.EvColors
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

/**
 * Premium Station details + services – redesigned.
 */
@Composable
fun StationDetailScreen(
    stationId: String,
    onBack: () -> Unit,
    onBookService: (stationId: String, serviceId: String) -> Unit,
    onGetDirections: (latitude: Double, longitude: Double, name: String) -> Unit,
    /** Opens the report-a-problem form with this station attached. */
    onReportIssue: (stationId: String, stationName: String) -> Unit = { _, _ -> }
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
        // Reviews are supplementary — a failure here must not blank the screen.
        StationRepository().stationReviews(stationId).fold(
            onSuccess = { reviews = it },
            onFailure = { }
        )
        loading = false
    }

    when {
        loading -> Column(
            Modifier.fillMaxSize().background(EvColors.Background)
        ) {
            StationAppBar(onBack = onBack, showProfile = false)
            Box(
                Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = EvColors.Primary, strokeWidth = 2.dp)
                    Spacer(Modifier.height(12.dp))
                    Text("Loading station…", style = MaterialTheme.typography.bodySmall, color = EvColors.OnSurfaceVar)
                }
            }
        }

        error != null -> Column(
            Modifier.fillMaxSize().background(EvColors.Background)
        ) {
            StationAppBar(onBack = onBack, showProfile = false)
            Box(
                Modifier.fillMaxSize().padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Filled.WifiOff, null, tint = EvColors.OnSurfaceVar, modifier = Modifier.size(44.dp))
                    Spacer(Modifier.height(12.dp))
                    Text(error!!, color = EvColors.Error, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }

        station != null -> StationDetailContent(
            station!!, reviews, onBack, onBookService, onGetDirections, onReportIssue
        )
    }
}

@Composable
private fun StationAppBar(onBack: () -> Unit, showProfile: Boolean = true) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        FlowBackButton(onBack = onBack)
        Spacer(Modifier.width(12.dp))
        Text("Station Details", style = MaterialTheme.typography.titleLarge, color = EvColors.OnBackground, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
        if (showProfile) {
            Box(
                Modifier.size(34.dp).clip(RoundedCornerShape(50)).background(EvColors.PrimaryDim),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.Person, null, tint = EvColors.Primary, modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
private fun FlowBackButton(onBack: () -> Unit) {
    Box(
        Modifier
            .size(36.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(EvColors.SurfaceHigh)
            .border(1.dp, EvColors.SurfaceBorder, RoundedCornerShape(10.dp))
            .clickable(onClick = onBack),
        contentAlignment = Alignment.Center
    ) {
        Icon(Icons.Filled.ArrowBack, null, tint = EvColors.OnSurface, modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun StationDetailContent(
    station: StationDto,
    reviews: StationReviewsDto?,
    onBack: () -> Unit,
    onBookService: (String, String) -> Unit,
    onGetDirections: (latitude: Double, longitude: Double, name: String) -> Unit,
    onReportIssue: (String, String) -> Unit
) {
    LazyColumn(
        Modifier
            .fillMaxSize()
            .background(EvColors.Background),
        contentPadding = PaddingValues(bottom = 32.dp)
    ) {
        // ── App bar ───────────────────────────────────────────────────────
        item {
            StationAppBar(onBack = onBack, showProfile = true)
        }

        // ── Station hero card ─────────────────────────────────────────────
        item {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(EvColors.Surface)
                    .border(1.dp, EvColors.SurfaceBorder, RoundedCornerShape(18.dp))
                    .padding(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(EvColors.PrimaryDim),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.EvStation, null, tint = EvColors.Primary, modifier = Modifier.size(22.dp))
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(station.name, style = MaterialTheme.typography.titleLarge, color = EvColors.OnBackground, fontWeight = FontWeight.Bold)
                        val r = reviews
                        if (r != null && r.count > 0) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("★", color = EvColors.Primary, style = MaterialTheme.typography.labelMedium)
                                Text(
                                    r.averageRating.toBigDecimal().stripTrailingZeros().toPlainString(),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = EvColors.OnBackground, fontWeight = FontWeight.SemiBold
                                )
                                Text("(${r.count})", style = MaterialTheme.typography.bodySmall, color = EvColors.OnSurfaceVar)
                            }
                        } else {
                            Text(
                                "${station.services.count { it.availableSlots > 0 }} of ${station.services.size} services open",
                                style = MaterialTheme.typography.bodySmall, color = EvColors.OnSurfaceVar
                            )
                        }
                    }
                    StatusPill(
                        if (station.status == "ACTIVE") "Active" else station.status.replace('_', ' ').lowercase(),
                        isActive = station.status == "ACTIVE"
                    )
                }
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Filled.CalendarMonth, null, tint = EvColors.OnSurfaceVar, modifier = Modifier.size(14.dp))
                    val open = station.openingTime?.take(5) ?: "--"
                    val close = station.closingTime?.take(5) ?: "--"
                    Text("Today • $open – $close", style = MaterialTheme.typography.bodySmall, color = EvColors.OnSurfaceVar)
                }
                station.address?.let {
                    Spacer(Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(Icons.Filled.LocationOn, null, tint = EvColors.OnSurfaceVar, modifier = Modifier.size(14.dp))
                        Text(it, style = MaterialTheme.typography.bodySmall, color = EvColors.OnSurfaceVar)
                    }
                }
            }
        }

        // ── Services ──────────────────────────────────────────────────────
        item {
            Spacer(Modifier.height(20.dp))
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Available Services", style = MaterialTheme.typography.titleMedium, color = EvColors.OnBackground, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                if (station.services.isEmpty()) {
                    Text("No services", style = MaterialTheme.typography.bodySmall, color = EvColors.OnSurfaceVar)
                }
            }
            Spacer(Modifier.height(10.dp))
        }

        if (station.services.isEmpty()) {
            item {
                Box(
                    Modifier.fillMaxWidth().padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No active services at this station.", color = EvColors.OnSurfaceVar)
                }
            }
        }

        items(station.services, key = { it.id }) { service ->
            Column(Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(EvColors.Surface)
                        .border(1.dp, EvColors.SurfaceBorder, RoundedCornerShape(16.dp))
                        .padding(16.dp)
                ) {
                    // Service type row
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).background(EvColors.PrimaryDim),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                if (service.serviceType == "BATTERY_SWAP") Icons.Filled.BatteryChargingFull else Icons.Filled.Bolt,
                                null, tint = EvColors.Primary, modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                if (service.serviceType == "BATTERY_SWAP") "Battery Swap"
                                else "Charging" + (service.powerKw?.let { " · ${it.toInt()} kW" } ?: ""),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = EvColors.OnBackground
                            )
                            service.connectorType?.let {
                                Text(it, style = MaterialTheme.typography.bodySmall, color = EvColors.OnSurfaceVar)
                            }
                        }
                        // Slots badge
                        val available = service.availableSlots
                        Box(
                            Modifier
                                .clip(RoundedCornerShape(50))
                                .background(if (available > 0) EvColors.PrimaryDim else EvColors.Error.copy(0.1f))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                if (available > 0) "$available slots" else "Full",
                                color = if (available > 0) EvColors.Primary else EvColors.Error,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    Spacer(Modifier.height(14.dp))

                    // Live availability bar: percentage of capacity free right now
                    val cap = service.capacitySlots ?: service.availableSlots
                    val frac = if (cap > 0) service.availableSlots.toFloat() / cap else 0f
                    val pct = (frac * 100).toInt()
                    val barColor = when {
                        frac > 0.5f  -> EvColors.Primary
                        frac > 0.15f -> EvColors.Warning
                        else         -> EvColors.Error
                    }
                    Column {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                if (service.serviceType == "BATTERY_SWAP") "CHARGED BATTERIES READY"
                                else "CHARGE POINTS FREE",
                                style = MaterialTheme.typography.labelSmall,
                                color = EvColors.OnSurfaceVar,
                                letterSpacing = 0.8.sp,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                "$pct%",
                                style = MaterialTheme.typography.titleSmall,
                                color = barColor,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(Modifier.height(6.dp))
                        com.example.evfinder.ui.components.EvProgressBar(fraction = frac, color = barColor)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "${service.availableSlots} of $cap " +
                                (if (service.serviceType == "BATTERY_SWAP") "swap bays free now" else "charge points free now"),
                            style = MaterialTheme.typography.labelSmall,
                            color = EvColors.OnSurfaceVar
                        )
                    }

                    Spacer(Modifier.height(14.dp))
                    HorizontalDivider(color = EvColors.SurfaceBorder.copy(0.4f))
                    Spacer(Modifier.height(14.dp))

                    // Price + CTA
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Price per unit", style = MaterialTheme.typography.labelSmall, color = EvColors.OnSurfaceVar)
                            Text(
                                "৳${service.pricePerUnit.toBigDecimal().stripTrailingZeros().toPlainString()}",
                                style = MaterialTheme.typography.headlineSmall,
                                color = EvColors.Primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        EvPrimaryButton(
                            text = "Book",
                            onClick = { onBookService(station.id, service.id) },
                            modifier = Modifier.width(120.dp),
                            enabled = service.availableSlots > 0,
                            icon = Icons.Filled.Bolt
                        )
                    }
                }
            }
        }

        // ── Reviews ───────────────────────────────────────────────────────
        item {
            Spacer(Modifier.height(20.dp))
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(Icons.Filled.Star, null, tint = EvColors.Secondary, modifier = Modifier.size(16.dp))
                Text("Reviews", style = MaterialTheme.typography.titleMedium, color = EvColors.OnBackground, fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.height(10.dp))
        }

        val r = reviews
        if (r == null || r.count == 0) {
            item {
                Box(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(EvColors.Surface)
                        .border(1.dp, EvColors.SurfaceBorder, RoundedCornerShape(14.dp))
                        .padding(16.dp)
                ) {
                    Text(
                        "No reviews yet — book a session and be the first to rate this station.",
                        color = EvColors.OnSurfaceVar,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        } else {
            item {
                // rating summary tile
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(EvColors.Surface)
                        .border(1.dp, EvColors.SurfaceBorder, RoundedCornerShape(14.dp))
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            r.averageRating.toBigDecimal().stripTrailingZeros().toPlainString(),
                            style = MaterialTheme.typography.headlineMedium,
                            color = EvColors.Primary, fontWeight = FontWeight.Bold
                        )
                        Text(
                            "★".repeat(r.averageRating.toInt().coerceIn(0, 5)),
                            color = EvColors.Primary,
                            style = MaterialTheme.typography.labelSmall
                        )
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
            items(r.reviews, key = { it.id }) { review ->
                Column(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 5.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(EvColors.SurfaceLow)
                        .border(1.dp, EvColors.SurfaceBorder.copy(0.6f), RoundedCornerShape(14.dp))
                        .padding(14.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier.size(30.dp).clip(CircleShape).background(EvColors.SurfaceHighest),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                review.userName.take(1).uppercase(),
                                color = EvColors.Primary,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                review.userName,
                                style = MaterialTheme.typography.labelLarge,
                                color = EvColors.OnSurface, fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                review.createdAt?.take(10) ?: "",
                                style = MaterialTheme.typography.labelSmall,
                                color = EvColors.OnSurfaceVar
                            )
                        }
                        Text(
                            "★".repeat(review.rating.coerceIn(0, 5)) +
                                "☆".repeat((5 - review.rating).coerceIn(0, 5)),
                            color = EvColors.Primary,
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                    review.comment?.let {
                        Spacer(Modifier.height(8.dp))
                        Text(it, style = MaterialTheme.typography.bodySmall, color = EvColors.OnSurface)
                    }
                }
            }
        }

        // ── Map placeholder ───────────────────────────────────────────────
        item {
            Spacer(Modifier.height(12.dp))
            Column(Modifier.padding(horizontal = 16.dp)) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF1C2B1A))
                ) {
                    // Live OpenStreetMap preview of the exact station position
                    AndroidView(
                        factory = { ctx ->
                            MapView(ctx).apply {
                                setTileSource(TileSourceFactory.MAPNIK)
                                setMultiTouchControls(false)
                                zoomController.setVisibility(
                                    org.osmdroid.views.CustomZoomButtonsController.Visibility.NEVER
                                )
                                isTilesScaledToDpi = true
                                controller.setZoom(15.0)
                                controller.setCenter(GeoPoint(station.latitude, station.longitude))
                            }
                        },
                        update = { map ->
                            if (map.tag != station.id) {
                                map.tag = station.id
                                map.overlays.clear()
                                map.overlays.add(Marker(map).apply {
                                    position = GeoPoint(station.latitude, station.longitude)
                                    title = station.name
                                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                                })
                                map.controller.setCenter(GeoPoint(station.latitude, station.longitude))
                                map.invalidate()
                            }
                        },
                        modifier = Modifier.matchParentSize()
                    )
                    Row(
                        Modifier
                            .align(Alignment.BottomStart)
                            .padding(12.dp)
                            .clip(RoundedCornerShape(50))
                            .background(EvColors.Background.copy(0.85f))
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Filled.LocationOn, null, tint = EvColors.Primary, modifier = Modifier.size(14.dp))
                        station.address?.let {
                            Text(it.take(28) + if (it.length > 28) "…" else "", style = MaterialTheme.typography.labelSmall, color = EvColors.OnSurface)
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                EvPrimaryButton(
                    text = "Get Directions",
                    onClick = {
                        onGetDirections(station.latitude, station.longitude, station.name)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    icon = Icons.Filled.Navigation
                )
                Spacer(Modifier.height(10.dp))
                // Report a problem with this specific station — goes to the admins
                EvOutlinedButton(
                    text = "Report a problem here",
                    onClick = { onReportIssue(station.id, station.name) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
