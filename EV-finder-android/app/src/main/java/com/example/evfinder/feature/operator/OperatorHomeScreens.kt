package com.example.evfinder.feature.operator

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Notifications
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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.evfinder.core.model.BookingDto
import com.example.evfinder.ui.components.EvCard
import com.example.evfinder.ui.components.EvStatTile
import com.example.evfinder.ui.components.EvPrimaryButton
import com.example.evfinder.ui.components.EvTopBar
import com.example.evfinder.ui.components.LiveDot
import com.example.evfinder.ui.components.SectionLabel
import com.example.evfinder.ui.components.StatusPill
import com.example.evfinder.ui.theme.EvColors
import java.time.LocalDate
import java.time.LocalTime

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun OperatorDashboardScreen(
    onSessionExpired: () -> Unit,
    onOpenNotifications: () -> Unit = {}
) {
    val vm: OperatorDashboardViewModel = viewModel()
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
                Text(state.error!!, color = EvColors.Error)
                Spacer(Modifier.height(12.dp))
                if (state.error!!.startsWith("Session expired")) EvPrimaryButton("Log in again", onSessionExpired)
                else TextButton(onClick = vm::load) { Text("Retry", color = EvColors.Primary) }
            }
            else -> {
                val greeting = when (LocalTime.now().hour) {
                    in 5..11 -> "Good morning"
                    in 12..16 -> "Good afternoon"
                    else -> "Good evening"
                }
                LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    item {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text("$greeting, ${state.operatorName} 👋",
                                    style = MaterialTheme.typography.headlineSmall, color = EvColors.OnBackground, fontWeight = FontWeight.Bold)
                                Text("Here's what's happening at your stations today",
                                    style = MaterialTheme.typography.bodySmall, color = EvColors.OnSurfaceVar)
                            }
                            LiveDot(true)
                            Spacer(Modifier.width(8.dp))
                            Box(
                                Modifier
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(11.dp))
                                    .background(EvColors.Surface)
                                    .clickable(onClick = onOpenNotifications),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Filled.Notifications, "Notifications",
                                    tint = EvColors.OnSurface, modifier = Modifier.size(19.dp))
                            }
                        }
                    }
                    // KPI grid with progress bars (mockup: operator_dashboard_mobile)
                    item {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            EvStatTile(
                                icon = Icons.Filled.EvStation,
                                caption = "Active stations",
                                value = state.activeStations.toString(),
                                unit = "/ ${state.totalStations}",
                                progress = if (state.totalStations > 0)
                                    state.activeStations.toFloat() / state.totalStations else 0f,
                                footer = "${state.totalStations - state.activeStations} offline",
                                modifier = Modifier.weight(1f)
                            )
                            EvStatTile(
                                icon = Icons.Filled.Today,
                                caption = "Today's bookings",
                                value = state.todaysBookings.toString(),
                                accent = EvColors.Secondary,
                                footer = "live today",
                                footerIcon = Icons.Filled.Bolt,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    item {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            EvStatTile(
                                icon = Icons.Filled.Payments,
                                caption = "Revenue",
                                value = "৳${state.revenue.toBigDecimal().stripTrailingZeros().toPlainString()}",
                                footer = "confirmed & completed",
                                modifier = Modifier.weight(1f)
                            )
                            EvStatTile(
                                icon = Icons.Filled.Speed,
                                caption = "Utilization",
                                value = "${state.utilizationPct}",
                                unit = "%",
                                progress = state.utilizationPct / 100f,
                                accent = EvColors.PrimaryFixedDim,
                                footer = "of daily capacity",
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    item { SectionLabel("Today's schedule", Modifier.padding(top = 8.dp)) }
                    val today = LocalDate.now().toString()
                    val todays = state.bookings.filter { it.startTime.startsWith(today) && it.status != "CANCELLED" }.sortedBy { it.startTime }
                    if (todays.isEmpty()) {
                        item {
                            EvCard(Modifier.fillMaxWidth()) {
                                Text("No bookings scheduled for today yet.", color = EvColors.OnSurfaceVar, modifier = Modifier.padding(16.dp))
                            }
                        }
                    }
                    items(todays.take(6), key = { it.id }) { ScheduleRow(it) }
                    item { SectionLabel("Live station status", Modifier.padding(top = 8.dp)) }
                    items(state.stations, key = { it.id }) { station ->
                        StationStatusRow(
                            name = station.name,
                            location = station.address ?: "",
                            services = station.services.size,
                            active = station.status == "ACTIVE",
                            statusLabel = if (station.status == "ACTIVE") "Operational" else station.status.lowercase().replace('_', ' ')
                        )
                    }
                }
            }
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
        Row(Modifier.padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(booking.startTime.substring(11, 16), style = MaterialTheme.typography.titleMedium, color = EvColors.Primary, fontWeight = FontWeight.Bold)
                Text(booking.endTime.substring(11, 16), style = MaterialTheme.typography.labelSmall, color = EvColors.OnSurfaceVar)
            }
            Spacer(Modifier.width(14.dp))
            Box(Modifier.width(1.dp).height(34.dp).background(EvColors.SurfaceBorder))
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(booking.stationName, style = MaterialTheme.typography.titleSmall, color = EvColors.OnBackground, fontWeight = FontWeight.SemiBold)
                Text("by ${booking.userName ?: "EV user"} · ${if (booking.serviceName == "BATTERY_SWAP") "Battery swap" else "Charging"}",
                    style = MaterialTheme.typography.labelSmall, color = EvColors.OnSurfaceVar)
            }
            StatusPill(label = label, isActive = active)
        }
    }
}

@Composable
private fun StationStatusRow(name: String, location: String, services: Int, active: Boolean, statusLabel: String) {
    EvCard(Modifier.fillMaxWidth()) {
        Row(Modifier.padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(38.dp).clip(RoundedCornerShape(10.dp)).background(EvColors.PrimaryDim), contentAlignment = Alignment.Center) {
                Text("⚡")
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(name, style = MaterialTheme.typography.titleSmall, color = EvColors.OnBackground, fontWeight = FontWeight.SemiBold)
                Text((location.take(28) + if (location.length > 28) "…" else "") + " · $services services",
                    style = MaterialTheme.typography.labelSmall, color = EvColors.OnSurfaceVar)
            }
            StatusPill(label = statusLabel, isActive = active)
        }
    }
}

@Composable
fun OperatorBookingsScreen(onSessionExpired: () -> Unit) {
    val vm: OperatorBookingsViewModel = viewModel()
    val state by vm.uiState.collectAsState()

    Column(Modifier.fillMaxSize().background(EvColors.Background)) {
        EvTopBar(title = "Bookings", subtitle = "User reservations at your stations")
        when {
            state.loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = EvColors.Primary)
            }
            state.error != null -> Column(
                Modifier.fillMaxSize().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(state.error!!, color = EvColors.Error)
                Spacer(Modifier.height(12.dp))
                if (state.error!!.startsWith("Session expired")) EvPrimaryButton("Log in again", onSessionExpired)
                else TextButton(onClick = vm::load) { Text("Retry", color = EvColors.Primary) }
            }
            state.bookings.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No bookings yet", color = EvColors.OnSurfaceVar)
            }
            else -> LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(state.bookings, key = { it.id }) { OperatorBookingCard(it) }
            }
        }
    }
}

@Composable
private fun OperatorBookingCard(booking: BookingDto) {
    val (label, active) = when (booking.status) {
        "CONFIRMED" -> "Confirmed" to true
        "PENDING" -> "Pending payment" to true
        "COMPLETED" -> "Completed" to false
        else -> "Cancelled" to false
    }
    EvCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(booking.stationName, style = MaterialTheme.typography.titleMedium, color = EvColors.OnBackground, fontWeight = FontWeight.SemiBold)
                    Text("${booking.startTime.take(10)} · ${booking.startTime.substring(11, 16)}–${booking.endTime.substring(11, 16)}",
                        style = MaterialTheme.typography.bodySmall, color = EvColors.OnSurfaceVar)
                }
                StatusPill(label = label, isActive = active)
            }
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(26.dp).clip(RoundedCornerShape(50)).background(EvColors.SurfaceHigh), contentAlignment = Alignment.Center) {
                    Text((booking.userName ?: "U").take(1).uppercase(), color = EvColors.Primary, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.width(8.dp))
                Text(booking.userName ?: "EV user", style = MaterialTheme.typography.bodyMedium, color = EvColors.OnSurface)
                Spacer(Modifier.weight(1f))
                booking.amount?.let {
                    Text("৳${it.toBigDecimal().stripTrailingZeros().toPlainString()}", color = EvColors.Primary, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}
