@file:OptIn(ExperimentalLayoutApi::class)

package com.example.evfinder.feature.home

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.evfinder.core.model.BookingDto
import com.example.evfinder.core.model.FuelStationDto
import com.example.evfinder.core.model.ServiceDto
import com.example.evfinder.core.model.StationDto
import com.example.evfinder.feature.fuel.FuelRepository
import com.example.evfinder.feature.fuel.FuelStationCard
import com.example.evfinder.ui.components.*
import com.example.evfinder.ui.theme.EvColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

/**
 * Find Stations / Dashboard tab – premium redesign.
 */
@Composable
fun HomeScreen(
    onStationClick: (String) -> Unit,
    onFuelStationClick: (String) -> Unit,
    onOpenMap: () -> Unit,
    onSessionExpired: () -> Unit,
    viewModel: HomeViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()

    // ── Fuel Station module state (separate from EV stations, no booking) ──
    var fuelMode by remember { mutableStateOf(false) }
    var fuelQuery by remember { mutableStateOf("") }
    var fuelFilter by remember { mutableStateOf<String?>(null) }        // LPG/DIESEL/OCTANE/PETROL
    var fuelAvailableOnly by remember { mutableStateOf(false) }
    var fuelStations by remember { mutableStateOf<List<FuelStationDto>?>(null) }
    var fuelLoading by remember { mutableStateOf(false) }
    var fuelError by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    fun loadFuelStations() {
        scope.launch {
            fuelLoading = true
            fuelError = null
            FuelRepository().getFuelStations(
                query = fuelQuery.takeIf { it.isNotBlank() },
                fuel = fuelFilter,
                availableOnly = fuelAvailableOnly
            ).fold(
                onSuccess = { fuelStations = it; fuelLoading = false },
                onFailure = { fuelError = it.message; fuelLoading = false }
            )
        }
    }

    LaunchedEffect(fuelMode, fuelQuery, fuelFilter, fuelAvailableOnly) {
        if (fuelMode) {
            delay(250) // tiny debounce for typed searches
            loadFuelStations()
        }
    }

    // Re-fetch silently every time the user lands on this tab, so the station
    // list and the upcoming-booking banner always reflect the latest state.
    LaunchedEffect(Unit) {
        if (viewModel.uiState.value.stations.isNotEmpty()) viewModel.silentRefresh()
    }

    LazyColumn(
        Modifier
            .fillMaxSize()
            .background(EvColors.Background),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        // ── App bar ───────────────────────────────────────────────────────
        item {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Logo
                Box(
                    Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(EvColors.PrimaryDim)
                        .border(1.dp, EvColors.Primary.copy(0.35f), RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.Bolt, null, tint = EvColors.Primary, modifier = Modifier.size(20.dp))
                }
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text("EV FINDER", style = MaterialTheme.typography.labelSmall, color = EvColors.Primary, letterSpacing = 1.sp)
                    Text("Find Stations", style = MaterialTheme.typography.titleMedium, color = EvColors.OnBackground, fontWeight = FontWeight.Bold)
                }
                // Map shortcut (fullscreen osmdroid map with station markers)
                Box(
                    Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(EvColors.SurfaceHigh)
                        .border(1.dp, EvColors.SurfaceBorder, RoundedCornerShape(10.dp))
                        .clickable(onClick = onOpenMap),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Outlined.Map, null, tint = EvColors.OnSurface, modifier = Modifier.size(18.dp))
                }
                Spacer(Modifier.width(8.dp))
                // Live badge
                Row(
                    Modifier
                        .clip(RoundedCornerShape(50))
                        .background(EvColors.SurfaceHigh)
                        .border(1.dp, EvColors.SurfaceBorder, RoundedCornerShape(50))
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    LiveDot(state.liveUpdates)
                    Spacer(Modifier.width(5.dp))
                    Text(
                        if (state.liveUpdates) "Live" else "Offline",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (state.liveUpdates) EvColors.Primary else EvColors.OnSurfaceVar
                    )
                }
            }
        }

        // ── Search bar ────────────────────────────────────────────────────
        item {
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
                BasicSearchField(
                    value = if (fuelMode) fuelQuery else state.query,
                    onValueChange = { if (fuelMode) fuelQuery = it else viewModel.onQueryChanged(it) },
                    placeholder = "Search stations",
                    modifier = Modifier.weight(1f)
                )
                Icon(Icons.Outlined.MyLocation, null, tint = EvColors.Primary, modifier = Modifier.size(20.dp))
            }
        }

        // ── Station category toggle: [ EV Charging ] [ Fuel Station ] ──────
        item {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(EvColors.SurfaceHigh)
                    .border(1.dp, EvColors.SurfaceBorder, RoundedCornerShape(12.dp)),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                StationCategoryPill("EV Charging", Icons.Filled.EvStation, !fuelMode, Modifier.weight(1f)) {
                    fuelMode = false
                }
                StationCategoryPill("Fuel Station", Icons.Filled.LocalGasStation, fuelMode, Modifier.weight(1f)) {
                    fuelMode = true
                }
            }
        }

        if (!fuelMode) {
        // ── Filter chips + live map preview (EV / Fuel / LPG layers) ───────
        item {
            HomeFiltersAndPreview(
                stations = state.stations,
                pois = state.pois,
                onOpenMap = onOpenMap
            )
        }

        // ── Upcoming booking preview (if any) ────────────────────────────
        val upcoming = state.upcoming
        if (upcoming != null) {
            item {
                Spacer(Modifier.height(4.dp))
                UpcomingBannerCard(upcoming, modifier = Modifier.padding(horizontal = 16.dp))
            }
        }

        // ── Stations header ───────────────────────────────────────────────
        item {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    if (state.query.isBlank()) "Nearby Stations" else "Search results",
                    style = MaterialTheme.typography.titleMedium,
                    color = EvColors.OnBackground,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
                if (state.loading) CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp, color = EvColors.Primary)
            }
        }

        // ── Station list ──────────────────────────────────────────────────
        when {
            state.error != null -> item {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Filled.WifiOff, null, tint = EvColors.OnSurfaceVar, modifier = Modifier.size(40.dp))
                    Spacer(Modifier.height(12.dp))
                    Text(state.error!!, color = EvColors.Error, style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(12.dp))
                    if (state.error!!.startsWith("Session expired")) {
                        EvPrimaryButton("Log in again", onClick = onSessionExpired, modifier = Modifier.fillMaxWidth(0.6f))
                    } else {
                        TextButton(onClick = viewModel::refresh) {
                            Text("Retry", color = EvColors.Primary)
                        }
                    }
                }
            }

            state.stations.isEmpty() && !state.loading -> item {
                Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Filled.SearchOff, null, tint = EvColors.OnSurfaceVar, modifier = Modifier.size(40.dp))
                        Spacer(Modifier.height(12.dp))
                        Text(
                            if (state.query.isBlank()) "No active stations yet"
                            else "No stations match \"${state.query}\"",
                            color = EvColors.OnSurfaceVar
                        )
                    }
                }
            }

            else -> {
                // Featured top station (full card)
                val featured = state.stations.firstOrNull()
                if (featured != null) {
                    item {
                        FeaturedStationCard(
                            station = featured,
                            onClick = { onStationClick(featured.id) },
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                }
                // Remaining as compact list rows
                if (state.stations.size > 1) {
                    items(state.stations.drop(1), key = { it.id }) { station ->
                        CompactStationRow(
                            station = station,
                            onClick = { onStationClick(station.id) },
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                        HorizontalDivider(
                            Modifier.padding(horizontal = 16.dp),
                            color = EvColors.SurfaceBorder.copy(0.5f)
                        )
                    }
                }
            }
        }
        } else {
            // ── Fuel Station mode: queue / remaining liters / price — NO booking ──
            item {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Fuel Stations",
                        style = MaterialTheme.typography.titleMedium,
                        color = EvColors.OnBackground,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f)
                    )
                    if (fuelLoading) CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp, color = EvColors.Primary)
                }
            }
            item {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FuelFilterChip("All", fuelFilter == null, Modifier.weight(1f)) { fuelFilter = null }
                    FuelFilterChip("LPG", fuelFilter == "LPG", Modifier.weight(1f)) { fuelFilter = "LPG" }
                    FuelFilterChip("Diesel", fuelFilter == "DIESEL", Modifier.weight(1f)) { fuelFilter = "DIESEL" }
                    FuelFilterChip("Octane", fuelFilter == "OCTANE", Modifier.weight(1f)) { fuelFilter = "OCTANE" }
                    FuelFilterChip("Petrol", fuelFilter == "PETROL", Modifier.weight(1f)) { fuelFilter = "PETROL" }
                }
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                    FuelFilterChip("In stock only", fuelAvailableOnly, Modifier.weight(1f)) {
                        fuelAvailableOnly = !fuelAvailableOnly
                    }
                }
            }
            when {
                fuelError != null -> item {
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Filled.WifiOff, null, tint = EvColors.OnSurfaceVar, modifier = Modifier.size(40.dp))
                        Spacer(Modifier.height(12.dp))
                        Text(fuelError!!, color = EvColors.Error, style = MaterialTheme.typography.bodyMedium)
                        Spacer(Modifier.height(12.dp))
                        if (fuelError!!.startsWith("Session expired")) {
                            EvPrimaryButton("Log in again", onClick = onSessionExpired, modifier = Modifier.fillMaxWidth(0.6f))
                        } else {
                            TextButton(onClick = { loadFuelStations() }) { Text("Retry", color = EvColors.Primary) }
                        }
                    }
                }

                fuelStations != null && fuelStations!!.isEmpty() && !fuelLoading -> item {
                    Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Filled.SearchOff, null, tint = EvColors.OnSurfaceVar, modifier = Modifier.size(40.dp))
                            Spacer(Modifier.height(12.dp))
                            Text(
                                if (fuelQuery.isBlank()) "No fuel stations yet"
                                else "No fuel stations match \"$fuelQuery\"",
                                color = EvColors.OnSurfaceVar
                            )
                        }
                    }
                }

                else -> items(fuelStations ?: emptyList(), key = { it.id }) { fs ->
                    FuelStationCard(
                        station = fs,
                        onClick = { onFuelStationClick(fs.id) },
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }
        }
    }
}

// ─── Fuel station category toggle + filter chips ─────────────────────────────
@Composable
private fun StationCategoryPill(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Row(
        modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) EvColors.Primary else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = if (selected) EvColors.OnPrimary else EvColors.OnSurfaceVar, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(6.dp))
        Text(
            label,
            color = if (selected) EvColors.OnPrimary else EvColors.OnSurfaceVar,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}

@Composable
private fun FuelFilterChip(label: String, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier
            .clip(RoundedCornerShape(50))
            .background(if (selected) EvColors.Primary else EvColors.SurfaceHigh)
            .border(1.dp, if (selected) EvColors.Primary else EvColors.SurfaceBorder, RoundedCornerShape(50))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            label,
            color = if (selected) EvColors.OnPrimary else EvColors.OnSurface,
            style = MaterialTheme.typography.labelMedium,
            maxLines = 1
        )
    }
}

// ─── Featured full station card ───────────────────────────────────────────────
@Composable
private fun FeaturedStationCard(station: StationDto, onClick: () -> Unit, modifier: Modifier = Modifier) {
    EvCard(modifier = modifier.fillMaxWidth(), onClick = onClick) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        station.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = EvColors.OnBackground
                    )
                    station.address?.let {
                        Spacer(Modifier.height(3.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Navigation, null, tint = EvColors.Primary, modifier = Modifier.size(12.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(it, style = MaterialTheme.typography.bodySmall, color = EvColors.OnSurfaceVar)
                        }
                    }
                }
                Icon(Icons.Outlined.FavoriteBorder, null, tint = EvColors.OnSurfaceVar, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.height(14.dp))

            // Stat row
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatPill(icon = Icons.Filled.Bolt, label = "350 kW", sublabel = "UltraFast")
                StatPill(icon = Icons.Filled.Cable, label = "CCS ×4", sublabel = "NACS / CCS")
                StatPill(icon = Icons.Filled.LocalGasStation, label = "${station.fuelLevel}%", sublabel = "Fuel level")
                StatusPill(label = "2 of 4 Open")
            }

            Spacer(Modifier.height(12.dp))
            HorizontalDivider(color = EvColors.SurfaceBorder.copy(0.4f))
            Spacer(Modifier.height(12.dp))

            // Amenities row
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                station.services.firstOrNull()?.let {
                    Text(
                        "৳${it.pricePerUnit.toBigDecimal().stripTrailingZeros().toPlainString()} / kWh",
                        style = MaterialTheme.typography.labelMedium,
                        color = EvColors.OnSurface
                    )
                }
                AmenityTag(Icons.Filled.LocalCafe, "Cafe")
                AmenityTag(Icons.Filled.Wifi, "Free WiFi")
                Spacer(Modifier.weight(1f))
                Text("No idle fee", style = MaterialTheme.typography.labelSmall, color = EvColors.Primary)
            }

            Spacer(Modifier.height(14.dp))

            // CTA
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                EvPrimaryButton(
                    "Route & Reserve Stall",
                    onClick = onClick,
                    modifier = Modifier.weight(1f),
                    icon = Icons.Filled.Diamond
                )
                Box(
                    Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(EvColors.SurfaceHigh)
                        .border(1.dp, EvColors.SurfaceBorder, RoundedCornerShape(14.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Outlined.Share, null, tint = EvColors.OnSurfaceVar, modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}

// ─── Compact station row ─────────────────────────────────────────────────────
@Composable
private fun CompactStationRow(station: StationDto, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(EvColors.PrimaryDim),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Filled.EvStation, null, tint = EvColors.Primary, modifier = Modifier.size(22.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(station.name, style = MaterialTheme.typography.titleSmall, color = EvColors.OnBackground, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(2.dp))
            val kw = station.services.firstOrNull()?.powerKw?.toInt()?.toString() ?: "?"
            Text("$kw kW  •  ${station.services.size} Available  •  ${station.fuelLevel}% fuel", style = MaterialTheme.typography.bodySmall, color = EvColors.OnSurfaceVar)
        }
        Column(horizontalAlignment = Alignment.End) {
            LiveDot(station.status == "ACTIVE")
            Spacer(Modifier.height(4.dp))
            Icon(Icons.Filled.ChevronRight, null, tint = EvColors.OnSurfaceVar, modifier = Modifier.size(18.dp))
        }
    }
}

// ─── Upcoming booking banner ──────────────────────────────────────────────────
@Composable
private fun UpcomingBannerCard(booking: BookingDto, modifier: Modifier = Modifier) {
    Row(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(EvColors.PrimaryDim)
            .border(1.dp, EvColors.Primary.copy(0.3f), RoundedCornerShape(14.dp))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Filled.CalendarMonth, null, tint = EvColors.Primary, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text("Next booking", style = MaterialTheme.typography.labelSmall, color = EvColors.Primary, fontWeight = FontWeight.Bold)
            Text(booking.stationName, style = MaterialTheme.typography.titleSmall, color = EvColors.OnBackground, fontWeight = FontWeight.SemiBold)
            Text(
                "${booking.startTime.take(10)} · ${booking.startTime.substring(11, 16)}–${booking.endTime.substring(11, 16)}",
                style = MaterialTheme.typography.bodySmall, color = EvColors.OnSurfaceVar
            )
        }
        StatusPill(if (booking.status == "CONFIRMED") "Confirmed" else "Pending", booking.status == "CONFIRMED")
    }
}

// ─── Stat pill ────────────────────────────────────────────────────────────────
@Composable
private fun StatPill(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    sublabel: String
) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = EvColors.Primary, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(4.dp))
            Text(label, style = MaterialTheme.typography.titleSmall, color = EvColors.OnBackground, fontWeight = FontWeight.Bold)
        }
        Text(sublabel, style = MaterialTheme.typography.labelSmall, color = EvColors.OnSurfaceVar)
    }
}

// ─── Amenity tag ──────────────────────────────────────────────────────────────
@Composable
private fun AmenityTag(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        Icon(icon, null, tint = EvColors.OnSurfaceVar, modifier = Modifier.size(12.dp))
        Text(text, style = MaterialTheme.typography.labelSmall, color = EvColors.OnSurfaceVar)
    }
}

// ─── Minimal text-only search input ──────────────────────────────────────────
@Composable
private fun BasicSearchField(
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

// ─── Filter chips + live map preview (EV / Fuel / LPG layers) ────────────────
@Composable
private fun HomeFiltersAndPreview(
    stations: List<StationDto>,
    pois: List<com.example.evfinder.core.network.OverpassClient.Poi>,
    onOpenMap: () -> Unit
) {
    // chip state is local to this composable, so the item block above doesn't
    // reset it and the map preview below always sees the latest values
    var showEv by remember { mutableStateOf(true) }
    var showFuel by remember { mutableStateOf(true) }
    var showLpg by remember { mutableStateOf(true) }

    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item { FilterChipPill("⚡ Available Now", Icons.Filled.Bolt, showEv) { showEv = !showEv } }
        item { FilterChipPill("Fuel", Icons.Filled.LocalGasStation, showFuel) { showFuel = !showFuel } }
        item { FilterChipPill("LPG", Icons.Filled.LocalGasStation, showLpg) { showLpg = !showLpg } }
    }

    HomeMapPreview(
        stations = if (showEv) stations else emptyList(),
        fuelPois = pois.filter { !it.lpg && showFuel },
        lpgPois = pois.filter { it.lpg && showLpg },
        onOpenMap = onOpenMap
    )
}

// ─── Filter chip pill (EV / Fuel / LPG layers) ───────────────────────────────
@Composable
private fun FilterChipPill(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    onToggle: () -> Unit
) {
    Row(
        Modifier
            .clip(RoundedCornerShape(50))
            .background(if (selected) EvColors.Primary else EvColors.SurfaceHigh)
            .border(1.dp, if (selected) EvColors.Primary else EvColors.SurfaceBorder, RoundedCornerShape(50))
            .clickable(onClick = onToggle)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(icon, null, tint = if (selected) EvColors.OnPrimary else EvColors.OnSurfaceVar, modifier = Modifier.size(16.dp))
        Text(label, color = if (selected) EvColors.OnPrimary else EvColors.OnSurface, style = MaterialTheme.typography.labelMedium)
    }
}

// ─── Live map preview with EV pins + fuel/LPG dots ───────────────────────────
@Composable
private fun HomeMapPreview(
    stations: List<StationDto>,
    fuelPois: List<com.example.evfinder.core.network.OverpassClient.Poi>,
    lpgPois: List<com.example.evfinder.core.network.OverpassClient.Poi>,
    onOpenMap: () -> Unit
) {
    Box(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .height(200.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF1C2B1A))
    ) {
        AndroidView(
            factory = { ctx ->
                MapView(ctx).apply {
                    setTileSource(TileSourceFactory.MAPNIK)
                    setMultiTouchControls(false)
                    zoomController.setVisibility(
                        org.osmdroid.views.CustomZoomButtonsController.Visibility.NEVER
                    )
                    isTilesScaledToDpi = true
                    controller.setZoom(11.5)
                    controller.setCenter(GeoPoint(23.7810, 90.4150)) // Dhaka
                }
            },
            update = { map ->
                // Build a key from what we're actually drawing so the map
                // rebuilds when the data changes (chip toggles, POI load, etc.)
                val key = (stations.size * 100003) + (fuelPois.size * 137) + lpgPois.size
                if (map.tag as? Int != key) {
                    map.tag = key
                    map.overlays.clear()
                    val ctx = map.context
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
                            icon = com.example.evfinder.ui.components.evCircleMarker(
                                ctx, android.graphics.Color.rgb(255, 167, 38)
                            )
                            title = "⛽ ${poi.name}"
                            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                        })
                    }
                    lpgPois.forEach { poi ->
                        map.overlays.add(Marker(map).apply {
                            position = GeoPoint(poi.lat, poi.lng)
                            icon = com.example.evfinder.ui.components.evCircleMarker(
                                ctx, android.graphics.Color.rgb(79, 195, 247)
                            )
                            title = "⛽ LPG · ${poi.name}"
                            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                        })
                    }
                    map.invalidate()
                }
            },
            modifier = Modifier.matchParentSize()
        )
        // touch-blocking tap layer: anywhere on the card opens the full map
        Spacer(
            Modifier
                .matchParentSize()
                .clickable(onClick = onOpenMap)
        )
        // hub count badge
        Row(
            Modifier
                .padding(12.dp)
                .clip(RoundedCornerShape(50))
                .background(EvColors.Background.copy(0.85f))
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            LiveDot()
            Spacer(Modifier.width(6.dp))
            Text("${stations.size} hubs nearby", style = MaterialTheme.typography.labelSmall, color = EvColors.OnSurface)
        }
        // location label
        Row(
            Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 12.dp)
                .clip(RoundedCornerShape(50))
                .background(EvColors.Background.copy(0.85f))
                .padding(horizontal = 14.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(Icons.Filled.LocationOn, null, tint = EvColors.Primary, modifier = Modifier.size(14.dp))
            Text("Tap to explore map", style = MaterialTheme.typography.labelSmall, color = EvColors.OnSurface)
        }
    }
}
