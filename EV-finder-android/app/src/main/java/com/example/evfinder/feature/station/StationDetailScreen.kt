package com.example.evfinder.feature.station

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.example.evfinder.core.model.StationDto
import com.example.evfinder.ui.components.*
import com.example.evfinder.ui.theme.EvColors

/**
 * Premium Station details + services – redesigned.
 */
@Composable
fun StationDetailScreen(
    stationId: String,
    onBack: () -> Unit,
    onBookService: (stationId: String, serviceId: String) -> Unit,
    onGetDirections: (latitude: Double, longitude: Double, name: String) -> Unit
) {
    var station by remember { mutableStateOf<StationDto?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(true) }

    LaunchedEffect(stationId) {
        StationRepository().getStation(stationId).fold(
            onSuccess = { station = it },
            onFailure = { error = it.message }
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

        station != null -> StationDetailContent(station!!, onBack, onBookService, onGetDirections)
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
    onBack: () -> Unit,
    onBookService: (String, String) -> Unit,
    onGetDirections: (latitude: Double, longitude: Double, name: String) -> Unit
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
                        Text("Bay 04", style = MaterialTheme.typography.bodySmall, color = EvColors.OnSurfaceVar)
                    }
                    StatusPill("Reserved", isActive = false)
                }
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Filled.CalendarMonth, null, tint = EvColors.OnSurfaceVar, modifier = Modifier.size(14.dp))
                    val open = station.openingTime?.take(5) ?: "--"
                    val close = station.closingTime?.take(5) ?: "--"
                    Text("Today • $open – $close (45 min)", style = MaterialTheme.typography.bodySmall, color = EvColors.OnSurfaceVar)
                }
                station.address?.let {
                    Spacer(Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(Icons.Filled.LocationOn, null, tint = EvColors.OnSurfaceVar, modifier = Modifier.size(14.dp))
                        Text(it, style = MaterialTheme.typography.bodySmall, color = EvColors.OnSurfaceVar)
                    }
                }
                Spacer(Modifier.height(12.dp))
                val level = station.fuelLevel
                val levelColor = when {
                    level >= 50 -> EvColors.Success
                    level >= 20 -> EvColors.Warning
                    else -> EvColors.Error
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.LocalGasStation, null, tint = levelColor, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "Fuel level",
                        style = MaterialTheme.typography.bodySmall,
                        color = EvColors.OnSurfaceVar,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        "$level%",
                        style = MaterialTheme.typography.labelSmall,
                        color = levelColor,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Spacer(Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = { level / 100f },
                    modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                    color = levelColor,
                    trackColor = EvColors.SurfaceHigh
                )
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
                    Box(Modifier.fillMaxSize().background(
                        androidx.compose.ui.graphics.Brush.verticalGradient(
                            listOf(Color(0xFF0E1F0D), Color(0xFF1A2E19))
                        )
                    ))
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
            }
        }
    }
}
