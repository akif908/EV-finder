package com.example.evfinder.feature.operator

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.EvStation
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Today
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
import com.example.evfinder.core.model.BookingDto
import com.example.evfinder.ui.components.EvCard
import com.example.evfinder.ui.components.EvPrimaryButton
import com.example.evfinder.ui.components.EvTopBar
import com.example.evfinder.ui.components.LiveDot
import com.example.evfinder.ui.components.SectionLabel
import com.example.evfinder.ui.components.StatusPill
import com.example.evfinder.ui.theme.EvColors
import java.time.LocalDate
import java.time.LocalTime

/**
 * Operator dashboard (mockup layout): greeting, stat grid
 * (stations / today's bookings / revenue / utilization),
 * Today's Schedule and Live Station Status.
 */
@Composable
fun OperatorDashboardScreen(onSessionExpired: () -> Unit) {
    val vm: OperatorDashboardViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
    val state by vm.uiState.collectAsState()

    Column(Modifier.fillMaxSize().background(EvColors.Background)) {
        when {
            state.loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = EvColors.Primary)
            }

            state.error != null -> Column(
                Modifier.fillMaxSize().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(Icons.Filled.ErrorOutline, null, tint = EvColors.OnSurfaceVar, modifier = Modifier.size(44.dp))
                Spacer(Modifier.height(12.dp))
                Text(state.error!!, color = EvColors.Error, style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(12.dp))
                if (state.error!!.startsWith("Session expired")) {
                    EvPrimaryButton("Log in again", onSessionExpired)
                } else {
                    TextButton(onClick = vm::load) { Text("Retry", color = EvColors.Primary) }
                }
            }

            else -> {
                val greeting = when (LocalTime.now().hour) {
                    in 5..11 -> "Good morning"
                    in 12..16 -> "Good afternoon"
                    else -> "Good evening"
                }
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // ---- greeting ----
                    item {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    "$greeting, ${state.operatorName} 👋",
                                    style = MaterialTheme.typography.headlineSmall,
                                    color = EvColors.OnBackground,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    "Here's what's happening at your stations today",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = EvColors.OnSurfaceVar
                                )
                            }
                            LiveDot(true)
                        }
                    }

                    // ---- stat grid (2x2, mockup style) ----
                    item {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            StatTile(Icons.Filled.EvStation, "Active Stations",
                                "${state.activeStations}/${state.totalStations}", Modifier.weight(1f))
                            StatTile(Icons.Filled.Today, "Today's Bookings",
                                state.todaysBookings.toString(), Modifier.weight(1f))
                        }
                    }
                    item {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            StatTile(Icons.Filled.Payments, "Revenue",
                                "৳${state.revenue.toBigDecimal().stripTrailingZeros().toPlainString()}",
                                Modifier.weight(1f))
                            StatTile(Icons.Filled.Speed, "Utilization",
                                "${state.utilizationPct}%", Modifier.weight(1f))
                        }
                    }

                    // ---- today's schedule ----
                    item { SectionLabel("Today's schedule", Modifier.padding(top = 8.dp)) }
                    val today = LocalDate.now().toString()
                    val todays = state.bookings.filter {
                        it.startTime.startsWith(today) && it.status != "CANCELLED"
                    }.sortedBy { it.startTime }
                    if (todays.isEmpty()) {
                        item {
                            EvCard(Modifier.fillMaxWidth()) {
                                Text(
                                    "No bookings scheduled for today yet.",
                                    color = EvColors.OnSurfaceVar,
                                    modifier = Modifier.padding(16.dp)
                                )
                            }
                        }
                    }
                    items(todays.take(6), key = { it.id }) { booking ->
                        ScheduleRow(booking)
                    }

                    // ---- live station status ----
                    item { SectionLabel("Live station status", Modifier.padding(top = 8.dp)) }
                    items(state.stations, key = { it.id }) { station ->
                        StationStatusRow(
                            name = station.name,
                            location = station.address ?: "",
                            services = station.services.size,
                            active = station.status == "ACTIVE",
                            statusLabel = if (station.status == "ACTIVE") "Operational"
                            else station.status.lowercase().replace('_', ' ')
                        )
                    }
                    item { Spacer(Modifier.height(8.dp)) }
                }
            }
        }
    }
}

@Composable
private fun StatTile(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    EvCard(modifier) {
        Column(Modifier.fillMaxWidth().padding(14.dp)) {
            Box(
                Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(EvColors.PrimaryDim),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = EvColors.Primary, modifier = Modifier.size(18.dp))
            }
            Spacer(Modifier.height(10.dp))
            Text(value, style = MaterialTheme.typography.titleLarge,
                color = EvColors.OnBackground, fontWeight = FontWeight.Bold)
            Text(label, style = MaterialTheme.typography.labelMedium, color = EvColors.OnSurfaceVar)
        }
    }
}

@Composable
private fun ScheduleRow(booking: BookingDto) {
    val (label, active) = when (booking.status) {
        "CONFIRMED" -> "Confirmed" to true
        "PENDING" -> "Pending" to true
        "COMPLETED" -> "Done" to false
        else -> "Cancelled" to false
    }
    EvCard(Modifier.fillMaxWidth()) {
        Row(
            Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // time block like the mockup
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    booking.startTime.substring(11, 16),
                    style = MaterialTheme.typography.titleMedium,
                    color = EvColors.Primary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    booking.endTime.substring(11, 16),
                    style = MaterialTheme.typography.labelSmall,
                    color = EvColors.OnSurfaceVar
                )
            }
            Spacer(Modifier.width(14.dp))
            Box(Modifier.width(1.dp).height(34.dp).background(EvColors.SurfaceBorder))
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(booking.stationName, style = MaterialTheme.typography.titleSmall,
                    color = EvColors.OnBackground, fontWeight = FontWeight.SemiBold)
                Text(
                    "by ${booking.userName ?: "EV user"} · ${if (booking.serviceName == "BATTERY_SWAP") "Battery swap" else "Charging"}",
                    style = MaterialTheme.typography.labelSmall, color = EvColors.OnSurfaceVar
                )
            }
            StatusPill(label = label, isActive = active)
        }
    }
}

@Composable
private fun StationStatusRow(name: String, location: String, services: Int, active: Boolean, statusLabel: String) {
    EvCard(Modifier.fillMaxWidth()) {
        Row(
            Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(EvColors.PrimaryDim),
                contentAlignment = Alignment.Center
            ) {
                Text("⚡", color = androidx.compose.ui.graphics.Color.Unspecified)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(name, style = MaterialTheme.typography.titleSmall,
                    color = EvColors.OnBackground, fontWeight = FontWeight.SemiBold)
                Text(
                    (location.take(28) + if (location.length > 28) "…" else "") + " · $services services",
                    style = MaterialTheme.typography.labelSmall, color = EvColors.OnSurfaceVar
                )
            }
            StatusPill(label = statusLabel, isActive = active)
        }
    }
}
