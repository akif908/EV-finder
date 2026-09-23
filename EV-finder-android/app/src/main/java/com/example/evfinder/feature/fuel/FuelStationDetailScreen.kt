package com.example.evfinder.feature.fuel

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.evfinder.core.model.FuelStationDto
import com.example.evfinder.ui.components.EvPrimaryButton
import com.example.evfinder.ui.components.StatusPill
import com.example.evfinder.ui.theme.EvColors

/**
 * Fuel Station details — queue, remaining liters, BDT price and last-updated
 * time per fuel type. NO booking: fuel stations are informational only.
 */
@Composable
fun FuelStationDetailScreen(
    fuelStationId: String,
    onBack: () -> Unit,
    onGetDirections: (latitude: Double, longitude: Double, name: String) -> Unit
) {
    var station by remember { mutableStateOf<FuelStationDto?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(true) }

    LaunchedEffect(fuelStationId) {
        FuelRepository().getFuelStation(fuelStationId).fold(
            onSuccess = { station = it },
            onFailure = { error = it.message }
        )
        loading = false
    }

    when {
        loading -> Column(Modifier.fillMaxSize().background(EvColors.Background)) {
            FuelAppBar(onBack)
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = EvColors.Primary, strokeWidth = 2.dp)
            }
        }

        error != null -> Column(Modifier.fillMaxSize().background(EvColors.Background)) {
            FuelAppBar(onBack)
            Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Filled.WifiOff, null, tint = EvColors.OnSurfaceVar, modifier = Modifier.size(44.dp))
                    Spacer(Modifier.height(12.dp))
                    Text(error!!, color = EvColors.Error, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }

        station != null -> FuelStationDetailContent(station!!, onBack, onGetDirections)
    }
}

@Composable
private fun FuelAppBar(onBack: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
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
        Spacer(Modifier.width(12.dp))
        Text("Fuel Station", style = MaterialTheme.typography.titleLarge,
            color = EvColors.OnBackground, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
        Box(
            Modifier.size(34.dp).clip(RoundedCornerShape(50)).background(EvColors.PrimaryDim),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Filled.Person, null, tint = EvColors.Primary, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
private fun FuelStationDetailContent(
    station: FuelStationDto,
    onBack: () -> Unit,
    onGetDirections: (Double, Double, String) -> Unit
) {
    LazyColumn(
        Modifier
            .fillMaxSize()
            .background(EvColors.Background),
        contentPadding = PaddingValues(bottom = 32.dp)
    ) {
        item { FuelAppBar(onBack) }

        // ── Hero card ─────────────────────────────────────────────────────
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
                        Icon(Icons.Filled.LocalGasStation, null, tint = EvColors.Primary, modifier = Modifier.size(22.dp))
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(station.name, style = MaterialTheme.typography.titleLarge,
                            color = EvColors.OnBackground, fontWeight = FontWeight.Bold)
                        station.description?.let {
                            Text(it, style = MaterialTheme.typography.bodySmall, color = EvColors.OnSurfaceVar)
                        }
                    }
                    StatusPill(label = if (station.isOpen) "Open" else "Closed", isActive = station.isOpen)
                }
                Spacer(Modifier.height(12.dp))
                station.address?.let {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.LocationOn, null, tint = EvColors.OnSurfaceVar, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(it, style = MaterialTheme.typography.bodySmall, color = EvColors.OnSurfaceVar)
                    }
                }
                if (!station.isOpen) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "This station is currently closed — prices and queues may be outdated.",
                        style = MaterialTheme.typography.bodySmall, color = EvColors.Warning
                    )
                }
            }
        }

        // ── Fuel info per type ────────────────────────────────────────────
        item {
            Spacer(Modifier.height(20.dp))
            Text(
                "Fuel Availability",
                style = MaterialTheme.typography.titleMedium,
                color = EvColors.OnBackground,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            if (station.inventories.isEmpty()) {
                Text(
                    "No fuel types listed for this station yet.",
                    style = MaterialTheme.typography.bodySmall,
                    color = EvColors.OnSurfaceVar,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
        }

        items(station.inventories.size) { index ->
            val inv = station.inventories[index]
            Column(Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(EvColors.Surface)
                        .border(1.dp, EvColors.SurfaceBorder, RoundedCornerShape(16.dp))
                        .padding(16.dp)
                ) {
                    val outOfStock = inv.remainingLiters <= 0.0
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            fuelLabel(inv.fuelType),
                            style = MaterialTheme.typography.titleMedium,
                            color = EvColors.OnBackground,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.weight(1f)
                        )
                        if (outOfStock) StatusPill(label = "Out of Stock", isActive = false)
                    }
                    Spacer(Modifier.height(10.dp))
                    HorizontalDivider(color = EvColors.SurfaceBorder.copy(0.4f))
                    Spacer(Modifier.height(10.dp))

                    InfoRow("Queue", "${inv.queueCount} vehicle" + if (inv.queueCount == 1) "" else "s")
                    InfoRow(
                        "Available",
                        if (outOfStock) "0 L" else "${liters(inv.remainingLiters)} L",
                        valueColor = if (outOfStock) EvColors.Error else EvColors.OnBackground
                    )
                    InfoRow("Price", "৳${bdt(inv.pricePerLiter)}/L", valueColor = EvColors.Primary)
                    inv.updatedAt?.let {
                        InfoRow("Last updated", it.replace('T', ' ').take(16), valueColor = EvColors.OnSurfaceVar)
                    }
                }
            }
        }

        // ── Map + directions ──────────────────────────────────────────────
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
                    Box(
                        Modifier.fillMaxSize().background(
                            Brush.verticalGradient(listOf(Color(0xFF0E1F0D), Color(0xFF1A2E19)))
                        )
                    )
                    Row(
                        Modifier
                            .align(Alignment.BottomStart)
                            .padding(12.dp)
                            .clip(RoundedCornerShape(50))
                            .background(EvColors.Background.copy(0.85f))
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.LocationOn, null, tint = EvColors.Primary, modifier = Modifier.size(14.dp))
                        station.address?.let {
                            Text(it.take(28) + if (it.length > 28) "…" else "",
                                style = MaterialTheme.typography.labelSmall, color = EvColors.OnSurface)
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                EvPrimaryButton(
                    text = "Get Directions",
                    onClick = { onGetDirections(station.latitude, station.longitude, station.name) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String, valueColor: Color = EvColors.OnBackground) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = EvColors.OnSurfaceVar)
        Text(value, style = MaterialTheme.typography.bodySmall, color = valueColor, fontWeight = FontWeight.SemiBold)
    }
}
