@file:OptIn(ExperimentalLayoutApi::class)

package com.example.evfinder.feature.home

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.evfinder.core.model.BookingDto
import com.example.evfinder.core.model.ServiceDto
import com.example.evfinder.core.model.StationDto

/**
 * Dashboard-style Home: greeting, live-availability badge, next booking,
 * quick stats and a stations section (searchable). Distinct from the
 * Bookings tab, which is the full booking history.
 */
@Composable
fun HomeScreen(
    onStationClick: (String) -> Unit,
    onSessionExpired: () -> Unit,
    viewModel: HomeViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // ---- Header ----
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Welcome back 👋", style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("EV Finder", style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold)
                }
                LiveBadge(live = state.liveUpdates, tick = state.liveTick)
            }
        }

        // ---- Next booking ----
        item {
            val upcoming = state.upcoming
            if (upcoming != null) {
                UpcomingBookingCard(upcoming)
            } else {
                Card(colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                    Column(Modifier.padding(16.dp)) {
                        Text("No upcoming booking", fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer)
                        Text("Pick a station below and grab a slot.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                }
            }
        }

        // ---- Quick stats ----
        item {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard("Stations", state.stations.size.toString(),
                    Modifier.weight(1f).height(72.dp))
                StatCard("My bookings", state.totalBookings.toString(),
                    Modifier.weight(1f).height(72.dp))
                StatCard("Available", countServices(state.stations),
                    Modifier.weight(1f).height(72.dp))
            }
        }

        // ---- Search ----
        item {
            OutlinedTextField(
                value = state.query,
                onValueChange = viewModel::onQueryChanged,
                placeholder = { Text("Search stations") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }

        // ---- Stations ----
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Stations near you", style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.weight(1f))
                if (state.loading) {
                    CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                }
            }
        }

        when {
            state.error != null -> item {
                Column(
                    Modifier.fillMaxWidth().padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(state.error!!, color = MaterialTheme.colorScheme.error)
                    Spacer(Modifier.height(12.dp))
                    if (state.error!!.startsWith("Session expired")) {
                        androidx.compose.material3.Button(onClick = onSessionExpired) {
                            Text("Log in again")
                        }
                    } else {
                        androidx.compose.material3.TextButton(onClick = viewModel::refresh) {
                            Text("Retry")
                        }
                    }
                }
            }

            state.stations.isEmpty() && !state.loading -> item {
                Text(
                    if (state.query.isBlank()) "No active stations yet"
                    else "No stations match \"${state.query}\"",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 24.dp)
                )
            }

            else -> items(state.stations, key = { it.id }) { station ->
                StationCard(station = station, onClick = { onStationClick(station.id) })
            }
        }
    }
}

@Composable
private fun LiveBadge(live: Boolean, tick: Int) {
    val color = if (live) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
    // pulsing dot while live; keyed on tick so each event restarts the pulse
    val pulse = rememberInfiniteTransition(label = "pulse")
    val alpha by pulse.animateFloat(
        initialValue = 1f, targetValue = 0.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(600), repeatMode = RepeatMode.Reverse
        ), label = "pulseAlpha"
    )
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Row(
            Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Spacer(
                Modifier
                    .size(8.dp)
                    .alpha(if (live) alpha else 0.5f)
                    .background(color, CircleShape)
            )
            Text(
                if (live) "Live · updates on" else "Live off",
                style = MaterialTheme.typography.labelMedium,
                color = color
            )
        }
    }
}

@Composable
private fun UpcomingBookingCard(booking: BookingDto) {
    Card(colors = CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(Modifier.padding(16.dp).animateContentSize()) {
            Text("Next booking", style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text(booking.stationName, style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold)
            Text(
                "${booking.startTime.take(10)} · ${booking.startTime.substring(11, 16)}–${booking.endTime.substring(11, 16)}" +
                    " · ${if (booking.status == "CONFIRMED") "Confirmed" else "Pending payment"}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun StatCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier = modifier, colors = CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.padding(12.dp)) {
            Text(value, style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Text(label, style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun StationCard(station: StationDto, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Bolt, contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.size(8.dp))
                Text(
                    station.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
                AssistChip(
                    onClick = {},
                    label = { Text(if (station.status == "ACTIVE") "Open" else station.status) }
                )
            }
            station.address?.let {
                Spacer(Modifier.height(4.dp))
                Text(it, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                station.services.take(3).forEach { ServiceChip(it) }
            }
        }
    }
}

@Composable
private fun ServiceChip(service: ServiceDto) {
    AssistChip(
        onClick = {},
        label = {
            val type = if (service.serviceType == "BATTERY_SWAP") "Swap" else
                (service.powerKw?.let { "${it.toInt()} kW" } ?: "Charging")
            Text("$type · ৳${service.pricePerUnit.toBigDecimal().stripTrailingZeros().toPlainString()}")
        }
    )
}

private fun countServices(stations: List<StationDto>): String {
    val total = stations.sumOf { it.services.size }
    return if (total > 0) "$total" else "0"
}
