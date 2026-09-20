@file:OptIn(ExperimentalLayoutApi::class)

package com.example.evfinder.feature.home

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Cable
import androidx.compose.material.icons.filled.EvStation
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.evfinder.core.model.BookingDto
import com.example.evfinder.core.model.StationDto
import com.example.evfinder.core.network.OverpassClient
import com.example.evfinder.ui.components.EvCard
import com.example.evfinder.ui.components.EvFilterChip
import com.example.evfinder.ui.components.LiveDot
import com.example.evfinder.ui.components.EvSpecTile
import com.example.evfinder.ui.components.StatusPill
import com.example.evfinder.ui.components.evCircleMarker
import com.example.evfinder.ui.theme.EvColors
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

/**
 * Find Stations — Voltage Mobility design: title row with station mark, pill
 * search bar, EV/Fuel/LPG filter chips, live map preview, upcoming booking
 * banner and rich station result cards.
 */
@Composable
fun HomeScreen(
    onStationClick: (String) -> Unit,
    onOpenMap: () -> Unit,
    onOpenNotifications: () -> Unit,
    onSessionExpired: () -> Unit,
    viewModel: HomeViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        if (viewModel.uiState.value.stations.isNotEmpty()) viewModel.silentRefresh()
    }

    LazyColumn(
        Modifier.fillMaxSize().background(EvColors.Background),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        item {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(EvColors.Primary.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.EvStation, null, tint = EvColors.Primary, modifier = Modifier.size(24.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("Find Stations", style = MaterialTheme.typography.headlineSmall,
                        color = EvColors.OnBackground, fontWeight = FontWeight.Bold)
                    Text(
                        if (state.liveUpdates) "Live availability on" else "Showing cached results",
                        style = MaterialTheme.typography.labelSmall, color = EvColors.OnSurfaceVar
                    )
                }
                IconAction(Icons.Filled.Notifications, "Notifications", onOpenNotifications)
                Spacer(Modifier.width(8.dp))
                IconAction(Icons.Outlined.Map, "Map", onOpenMap, EvColors.Primary)
            }
        }

        // search
        item {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(50))
                    .background(EvColors.SurfaceLowest)
                    .border(1.dp, EvColors.OutlineVariant.copy(alpha = 0.4f), RoundedCornerShape(50))
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.Search, null, tint = EvColors.OnSurfaceVar, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(10.dp))
                BasicTextField(
                    value = state.query,
                    onValueChange = viewModel::onQueryChanged,
                    modifier = Modifier.weight(1f),
                    textStyle = MaterialTheme.typography.bodyMedium.copy(color = EvColors.OnBackground),
                    singleLine = true,
                    cursorBrush = SolidColor(EvColors.Primary),
                    decorationBox = { inner ->
                        if (state.query.isEmpty()) {
                            Text("Search by location or station name…",
                                style = MaterialTheme.typography.bodyMedium,
                                color = EvColors.OnSurfaceVar.copy(alpha = 0.6f))
                        }
                        inner()
                    }
                )
                if (state.loading) {
                    CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp, color = EvColors.Primary)
                } else {
                    LiveDot(state.liveUpdates)
                }
            }
        }

        // chips + map preview
        item {
            HomeFiltersAndPreview(
                stations = state.stations,
                pois = state.pois,
                onOpenMap = onOpenMap
            )
        }

        // search filters / sorting
        item {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    EvFilterChip("Available now", state.onlyAvailable,
                        { viewModel.toggleOnlyAvailable() }, Icons.Filled.Bolt)
                }
                items(HomeViewModel.SORTS) { (mode, label) ->
                    EvFilterChip(label, state.sortMode == mode, { viewModel.setSort(mode) })
                }
            }
        }

        state.upcoming?.let { upcoming ->
            item {
                Spacer(Modifier.height(12.dp))
                UpcomingBanner(upcoming)
            }
        }

        item {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    if (state.query.isBlank()) "Nearby stations" else "Results",
                    style = MaterialTheme.typography.titleMedium,
                    color = EvColors.OnBackground, fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
                Text("${state.visibleStations.size} found",
                    style = MaterialTheme.typography.labelMedium, color = EvColors.OnSurfaceVar)
            }
        }

        when {
            state.error != null -> item {
                Column(
                    Modifier.fillMaxWidth().padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(state.error!!, color = EvColors.Error, style = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.height(12.dp))
                    SmallGreenButton(
                        if (state.error!!.startsWith("Session expired")) "Log in again" else "Retry",
                        if (state.error!!.startsWith("Session expired")) onSessionExpired else viewModel::refresh
                    )
                }
            }

            state.visibleStations.isEmpty() && !state.loading -> item {
                Column(
                    Modifier.fillMaxWidth().padding(vertical = 40.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Filled.EvStation, null, tint = EvColors.OnSurfaceVar,
                        modifier = Modifier.size(44.dp))
                    Spacer(Modifier.height(12.dp))
                    Text(
                        if (state.query.isBlank()) "No active stations yet"
                        else "No stations match \"${state.query}\"",
                        style = MaterialTheme.typography.bodyMedium, color = EvColors.OnSurfaceVar
                    )
                }
            }

            else -> items(state.visibleStations, key = { it.id }) { station ->
                StationResultCard(station, onClick = { onStationClick(station.id) })
            }
        }
    }
}

@Composable
private fun IconAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
    tint: Color = EvColors.OnSurface
) {
    Box(
        Modifier
            .size(40.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(EvColors.Surface)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, label, tint = tint, modifier = Modifier.size(20.dp))
    }
}

@Composable
private fun SmallGreenButton(text: String, onClick: () -> Unit) {
    Box(
        Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(EvColors.Primary)
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 10.dp)
    ) {
        Text(text, color = EvColors.OnPrimary, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
private fun HomeFiltersAndPreview(
    stations: List<StationDto>,
    pois: List<OverpassClient.Poi>,
    onOpenMap: () -> Unit
) {
    var showEv by remember { mutableStateOf(true) }
    var showFuel by remember { mutableStateOf(true) }
    var showLpg by remember { mutableStateOf(true) }

    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item { EvFilterChip("Available Now", showEv, { showEv = !showEv }, Icons.Filled.Bolt) }
        item { EvFilterChip("Fuel", showFuel, { showFuel = !showFuel }, Icons.Filled.LocalGasStation) }
        item { EvFilterChip("LPG", showLpg, { showLpg = !showLpg }, Icons.Filled.LocalGasStation) }
    }

    HomeMapPreview(
        stations = if (showEv) stations else emptyList(),
        fuelPois = pois.filter { !it.lpg && showFuel },
        lpgPois = pois.filter { it.lpg && showLpg },
        onOpenMap = onOpenMap
    )
}

@Composable
private fun HomeMapPreview(
    stations: List<StationDto>,
    fuelPois: List<OverpassClient.Poi>,
    lpgPois: List<OverpassClient.Poi>,
    onOpenMap: () -> Unit
) {
    Box(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .height(200.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(EvColors.SurfaceLowest)
    ) {
        AndroidView(
            factory = { ctx ->
                MapView(ctx).apply {
                    setTileSource(TileSourceFactory.MAPNIK)
                    setMultiTouchControls(false)
                    isTilesScaledToDpi = true
                    zoomController.setVisibility(
                        org.osmdroid.views.CustomZoomButtonsController.Visibility.NEVER
                    )
                    controller.setZoom(11.5)
                    controller.setCenter(GeoPoint(23.7810, 90.4150))
                }
            },
            update = { map ->
                val key = stations.size * 100003 + fuelPois.size * 137 + lpgPois.size
                if ((map.tag as? Int) != key) {
                    map.tag = key
                    map.overlays.clear()
                    val ctx: Context = map.context
                    stations.forEach { station ->
                        map.overlays.add(Marker(map).apply {
                            position = GeoPoint(station.latitude, station.longitude)
                            title = station.name
                            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                        })
                    }
                    fuelPois.forEach { poi ->
                        map.overlays.add(Marker(map).apply {
                            position = GeoPoint(poi.lat, poi.lng)
                            icon = evCircleMarker(ctx, android.graphics.Color.rgb(255, 167, 38))
                            title = "⛽ ${poi.name}"
                            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                        })
                    }
                    lpgPois.forEach { poi ->
                        map.overlays.add(Marker(map).apply {
                            position = GeoPoint(poi.lat, poi.lng)
                            icon = evCircleMarker(ctx, android.graphics.Color.rgb(79, 195, 247))
                            title = "⛽ LPG · ${poi.name}"
                            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                        })
                    }
                    map.invalidate()
                }
            },
            modifier = Modifier.matchParentSize()
        )

        Spacer(Modifier.matchParentSize().clickable(onClick = onOpenMap))

        Row(
            Modifier
                .align(Alignment.TopStart)
                .padding(12.dp)
                .clip(RoundedCornerShape(50))
                .background(EvColors.Background.copy(alpha = 0.85f))
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            LiveDot()
            Spacer(Modifier.width(6.dp))
            Text("${stations.size} EV · ${fuelPois.size + lpgPois.size} fuel/LPG",
                style = MaterialTheme.typography.labelSmall, color = EvColors.OnSurface)
        }

        Row(
            Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 12.dp)
                .clip(RoundedCornerShape(50))
                .background(EvColors.Primary)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(Icons.Filled.LocationOn, null, tint = EvColors.OnPrimary, modifier = Modifier.size(14.dp))
            Text("Tap to explore map", style = MaterialTheme.typography.labelMedium,
                color = EvColors.OnPrimary, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun UpcomingBanner(booking: BookingDto) {
    EvCard(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        color = EvColors.Primary.copy(alpha = 0.1f)
    ) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("NEXT BOOKING", style = MaterialTheme.typography.labelSmall,
                    color = EvColors.Primary, letterSpacing = 0.8.sp)
                Spacer(Modifier.weight(1f))
                StatusPill(
                    label = if (booking.status == "CONFIRMED") "Confirmed" else "Pending",
                    isActive = true
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(booking.stationName, style = MaterialTheme.typography.titleMedium,
                color = EvColors.OnBackground, fontWeight = FontWeight.SemiBold)
            Text(
                "${booking.startTime.take(10)} · ${booking.startTime.substring(11, 16)}–${booking.endTime.substring(11, 16)}",
                style = MaterialTheme.typography.bodySmall, color = EvColors.OnSurfaceVar
            )
        }
    }
}

@Composable
private fun RatingRow(rating: Double, count: Int) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (count == 0) {
            Text("Not rated yet", style = MaterialTheme.typography.labelSmall,
                color = EvColors.OnSurfaceVar)
        } else {
            Text("★", color = EvColors.Primary, style = MaterialTheme.typography.labelMedium)
            Spacer(Modifier.width(4.dp))
            Text(
                rating.toBigDecimal().stripTrailingZeros().toPlainString(),
                style = MaterialTheme.typography.labelMedium,
                color = EvColors.OnBackground, fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.width(6.dp))
            Text("($count)", style = MaterialTheme.typography.labelSmall, color = EvColors.OnSurfaceVar)
        }
    }
}

@Composable
private fun StationResultCard(station: StationDto, onClick: () -> Unit) {
    val available = station.services.count { it.availableSlots > 0 }
    val cheapest = station.services.minOfOrNull { it.pricePerUnit }

    EvCard(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
        onClick = onClick,
        color = EvColors.Surface
    ) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(station.name, style = MaterialTheme.typography.titleMedium,
                        color = EvColors.OnBackground, fontWeight = FontWeight.SemiBold, maxLines = 1)
                    station.address?.let {
                        Spacer(Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.LocationOn, null, tint = EvColors.OnSurfaceVar,
                                modifier = Modifier.size(12.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(it, style = MaterialTheme.typography.bodySmall,
                                color = EvColors.OnSurfaceVar, maxLines = 1)
                        }
                    }
                    // rating drives the ranking, so surface it on the card
                    Spacer(Modifier.height(6.dp))
                    RatingRow(station.averageRating, station.reviewCount)
                }
                StatusPill(
                    label = if (station.status == "ACTIVE") "Open" else station.status.lowercase().replace('_', ' '),
                    isActive = station.status == "ACTIVE"
                )
            }

            // 3-column spec bento (mockup: kW / connector / open stalls)
            val topPower = station.services.mapNotNull { it.powerKw }.maxOrNull()
            val connectors = station.services.mapNotNull { it.connectorType }.distinct()
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                EvSpecTile(
                    icon = Icons.Filled.Bolt,
                    value = topPower?.let { "${it.toInt()} kW" }
                        ?: (if (station.services.any { it.serviceType == "BATTERY_SWAP" }) "Swap" else "—"),
                    caption = if (topPower != null) "Max output" else "Service",
                    modifier = Modifier.weight(1f)
                )
                EvSpecTile(
                    icon = Icons.Filled.Cable,
                    value = connectors.firstOrNull()
                        ?: (if (station.services.any { it.serviceType == "BATTERY_SWAP" }) "Pod" else "—"),
                    caption = "Connector",
                    accent = EvColors.Secondary,
                    modifier = Modifier.weight(1f)
                )
                EvSpecTile(
                    icon = Icons.Filled.Bolt,
                    value = "${station.services.count { it.availableSlots > 0 }}/${station.services.size}",
                    caption = "Open now",
                    accent = if (available > 0) EvColors.Primary else EvColors.Error,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(Modifier.height(14.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    if (available > 0) "$available service${if (available == 1) "" else "s"} available"
                    else "All services busy",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (available > 0) EvColors.Primary else EvColors.Error
                )
                Spacer(Modifier.weight(1f))
                cheapest?.let {
                    Text(
                        "from ৳${it.toBigDecimal().stripTrailingZeros().toPlainString()}",
                        style = MaterialTheme.typography.labelLarge,
                        color = EvColors.OnBackground, fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}
