package com.example.evfinder.feature.admin

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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.EvStation
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.evfinder.core.model.BookingDto
import com.example.evfinder.ui.components.EvCard
import com.example.evfinder.ui.components.EvStatTile
import com.example.evfinder.ui.components.EvPrimaryButton
import com.example.evfinder.ui.components.EvTopBar
import com.example.evfinder.ui.components.SectionLabel
import com.example.evfinder.ui.components.StatusPill
import com.example.evfinder.ui.theme.EvColors

@Composable
fun AdminDashboardScreen(onSessionExpired: () -> Unit) {
    val vm: AdminDashboardViewModel = viewModel()
    val state by vm.uiState.collectAsState()
    Column(Modifier.fillMaxSize().background(EvColors.Background)) {
        EvTopBar(title = "Command Center", subtitle = "Platform overview")
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
                if (state.error!!.startsWith("Session expired")) EvPrimaryButton("Log in again", onSessionExpired)
                else TextButton(onClick = vm::load) { Text("Retry", color = EvColors.Primary) }
            }
            else -> {
                val o = state.overview
                LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    item {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            AdminStatTile("Users", o?.totalUsers?.toString() ?: "—", Modifier.weight(1f),
                                icon = Icons.Filled.People, accent = EvColors.Primary)
                            AdminStatTile("Operators", o?.totalOperators?.toString() ?: "—", Modifier.weight(1f),
                                icon = Icons.Filled.Storefront, accent = EvColors.Secondary)
                        }
                    }
                    item {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            AdminStatTile("Active stations", "${o?.activeStations ?: "—"}/${o?.totalStations ?: "—"}",
                                Modifier.weight(1f), icon = Icons.Filled.EvStation, accent = EvColors.Primary)
                            AdminStatTile("Bookings", o?.totalBookings?.toString() ?: "—", Modifier.weight(1f),
                                icon = Icons.Filled.CalendarMonth, accent = EvColors.Tertiary)
                        }
                    }
                    item {
                        EvCard(Modifier.fillMaxWidth()) {
                            Column(Modifier.fillMaxWidth().padding(16.dp)) {
                                Text("Total Revenue", style = MaterialTheme.typography.labelMedium, color = EvColors.OnSurfaceVar)
                                Text("৳${o?.totalRevenue?.toBigDecimal()?.stripTrailingZeros()?.toPlainString() ?: "—"}",
                                    style = MaterialTheme.typography.headlineMedium, color = EvColors.Primary, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    item { SectionLabel("Recent bookings", Modifier.padding(top = 8.dp)) }
                    items(state.recentBookings, key = { it.id }) { AdminBookingCard(it, null, false) }
                }
            }
        }
    }
}

@Composable
private fun AdminStatTile(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector = Icons.Filled.EvStation,
    accent: androidx.compose.ui.graphics.Color = EvColors.Primary,
    footer: String? = null
) {
    EvStatTile(
        icon = icon,
        caption = label,
        value = value,
        accent = accent,
        footer = footer,
        modifier = modifier
    )
}

@Composable
fun AdminUsersScreen(onSessionExpired: () -> Unit) {
    val vm: AdminUsersViewModel = viewModel()
    val state by vm.uiState.collectAsState()
    Column(Modifier.fillMaxSize().background(EvColors.Background)) {
        EvTopBar(title = "Users", subtitle = "Manage accounts & roles")
        Row(Modifier.padding(horizontal = 16.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(null to "All", "USER" to "Users", "OPERATOR" to "Operators", "ADMIN" to "Admins").forEach { (value, label) ->
                Text(
                    label,
                    color = if (state.roleFilter == value) EvColors.Primary else EvColors.OnSurfaceVar,
                    fontWeight = if (state.roleFilter == value) FontWeight.Bold else FontWeight.Normal,
                    modifier = Modifier.clip(RoundedCornerShape(50))
                        .background(if (state.roleFilter == value) EvColors.PrimaryDim else Color.Transparent)
                        .clickable { vm.setFilter(value) }.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }
        when {
            state.loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = EvColors.Primary)
            }
            state.error != null -> Column(Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(state.error!!, color = EvColors.Error)
                TextButton(onClick = vm::load) { Text("Retry", color = EvColors.Primary) }
            }
            else -> LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(state.users, key = { it.id }) { user ->
                    EvCard(Modifier.fillMaxWidth()) {
                        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(40.dp).clip(RoundedCornerShape(50)).background(EvColors.PrimaryDim), contentAlignment = Alignment.Center) {
                                Text(user.name.take(1).uppercase(), color = EvColors.Primary, fontWeight = FontWeight.Bold)
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(user.name, style = MaterialTheme.typography.titleSmall, color = EvColors.OnBackground, fontWeight = FontWeight.SemiBold)
                                Text(user.email, style = MaterialTheme.typography.bodySmall, color = EvColors.OnSurfaceVar)
                            }
                            StatusPill(label = user.role, isActive = user.role == "ADMIN")
                            IconButton(onClick = { vm.openRoleDialog(user) }, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Filled.Edit, "Change role", tint = EvColors.OnSurfaceVar, modifier = Modifier.size(16.dp))
                            }
                            IconButton(onClick = { vm.openDeleteDialog(user) }, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Filled.Delete, "Delete", tint = EvColors.Error, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }
        }
    }
    state.roleDialogUser?.let { user ->
        AlertDialog(
            onDismissRequest = vm::closeDialogs,
            containerColor = EvColors.Surface,
            title = { Text("Change role", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("${user.name} (${user.email})", style = MaterialTheme.typography.bodySmall, color = EvColors.OnSurfaceVar)
                    Spacer(Modifier.height(12.dp))
                    listOf("USER", "OPERATOR", "ADMIN").forEach { role ->
                        Row(
                            Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
                                .background(if (user.role == role) EvColors.PrimaryDim else EvColors.SurfaceHigh)
                                .clickable { vm.changeRole(user.id, role) }.padding(14.dp)
                        ) { Text(role, color = if (user.role == role) EvColors.Primary else EvColors.OnSurface) }
                        Spacer(Modifier.height(8.dp))
                    }
                }
            },
            confirmButton = { TextButton(onClick = vm::closeDialogs) { Text("Close", color = EvColors.OnSurfaceVar) } }
        )
    }
    state.deleteConfirmUser?.let { user ->
        AlertDialog(
            onDismissRequest = vm::closeDialogs,
            containerColor = EvColors.Surface,
            title = { Text("Delete user", fontWeight = FontWeight.Bold, color = EvColors.Error) },
            text = { Text("Delete ${user.name} (${user.email})? This cannot be undone.", color = EvColors.OnSurface) },
            confirmButton = {
                TextButton(onClick = { vm.deleteUser(user.id) }) { Text("Delete", color = EvColors.Error, fontWeight = FontWeight.Bold) }
            },
            dismissButton = { TextButton(onClick = vm::closeDialogs) { Text("Cancel", color = EvColors.OnSurfaceVar) } }
        )
    }
}

@Composable
fun AdminBookingsScreen(onSessionExpired: () -> Unit) {
    val vm: AdminBookingsViewModel = viewModel()
    val state by vm.uiState.collectAsState()
    Column(Modifier.fillMaxSize().background(EvColors.Background)) {
        EvTopBar(title = "Bookings", subtitle = "All platform reservations")
        when {
            state.loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = EvColors.Primary)
            }
            state.error != null -> Column(Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(state.error!!, color = EvColors.Error)
                TextButton(onClick = vm::load) { Text("Retry", color = EvColors.Primary) }
            }
            state.bookings.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No bookings yet", color = EvColors.OnSurfaceVar)
            }
            else -> LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(state.bookings, key = { it.id }) { booking ->
                    val cancellable = booking.status == "PENDING" || booking.status == "CONFIRMED"
                    AdminBookingCard(booking, if (cancellable) ({ vm.forceCancel(booking.id) }) else null, state.cancellingId == booking.id)
                }
            }
        }
    }
}

@Composable
private fun AdminBookingCard(booking: BookingDto, onCancel: (() -> Unit)?, cancelling: Boolean) {
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
                    Text("by ${booking.userName ?: "EV user"} · ${booking.startTime.take(10)} · ${booking.startTime.substring(11, 16)}–${booking.endTime.substring(11, 16)}",
                        style = MaterialTheme.typography.bodySmall, color = EvColors.OnSurfaceVar)
                }
                StatusPill(label = label, isActive = active)
            }
            if (onCancel != null) {
                Spacer(Modifier.height(10.dp))
                TextButton(onClick = onCancel, enabled = !cancelling) {
                    Text(if (cancelling) "Cancelling…" else "Force cancel (moderate)", color = EvColors.Error)
                }
            }
        }
    }
}
