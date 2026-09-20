@file:OptIn(ExperimentalLayoutApi::class)

package com.example.evfinder.feature.booking

import androidx.compose.animation.core.*
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.evfinder.core.model.BookingDto
import com.example.evfinder.core.network.AvailabilitySocket
import com.example.evfinder.ui.components.*
import com.example.evfinder.ui.theme.EvColors
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.LocalDateTime

data class BookingsUiState(
    val loading: Boolean = true,
    val bookings: List<BookingDto> = emptyList(),
    val filter: String = "ALL",
    val error: String? = null,
    val cancellingId: String? = null
)

class BookingsViewModel : ViewModel() {
    private val repository = BookingRepository()
    private val _uiState = MutableStateFlow(BookingsUiState())
    val uiState: StateFlow<BookingsUiState> = _uiState

    init {
        load()

        // Live updates: the backend broadcasts whenever a booking is created,
        // cancelled or a payment fails. Re-fetch silently so this list never
        // goes stale (e.g. right after the user pays on the payment screen).
        viewModelScope.launch {
            AvailabilitySocket.events.collect { load(silent = true) }
        }
        AvailabilitySocket.connect()
    }

    fun load(silent: Boolean = false) {
        viewModelScope.launch {
            if (!silent) _uiState.value = _uiState.value.copy(loading = true, error = null)
            repository.myBookings().fold(
                onSuccess = { b -> _uiState.value = _uiState.value.copy(loading = false, bookings = b) },
                onFailure = { e ->
                    // A background refresh that fails keeps the last good list
                    // instead of replacing it with an error screen.
                    _uiState.value = _uiState.value.copy(
                        loading = false,
                        error = if (silent && _uiState.value.bookings.isNotEmpty()) null else e.message
                    )
                }
            )
        }
    }

    fun setFilter(filter: String) { _uiState.value = _uiState.value.copy(filter = filter) }

    fun cancel(id: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(cancellingId = id)
            repository.cancelBooking(id).fold(
                onSuccess = { updated ->
                    _uiState.value = _uiState.value.copy(
                        cancellingId = null,
                        bookings = _uiState.value.bookings.map { if (it.id == updated.id) updated else it }
                    )
                },
                onFailure = { e -> _uiState.value = _uiState.value.copy(cancellingId = null, error = e.message) }
            )
        }
    }
}

@Composable
fun BookingsScreen(onSessionExpired: () -> Unit, viewModel: BookingsViewModel = viewModel()) {
    val state by viewModel.uiState.collectAsState()

    // Every time the user lands on this tab (e.g. right after a payment) the
    // list is refreshed silently, so the booking they just made is shown.
    LaunchedEffect(Unit) {
        if (viewModel.uiState.value.bookings.isNotEmpty()) viewModel.load(silent = true)
    }

    val visible = state.bookings.filter {
        when (state.filter) {
            "UPCOMING"  -> it.status == "PENDING" || it.status == "CONFIRMED"
            "COMPLETED" -> it.status == "COMPLETED"
            "CANCELLED" -> it.status == "CANCELLED"
            else        -> true
        }
    }

    // Derive active session for top banner
    val activeSession = state.bookings.firstOrNull { it.status == "CONFIRMED" &&
        runCatching { LocalDateTime.parse(it.startTime).isBefore(LocalDateTime.now()) &&
            LocalDateTime.parse(it.endTime).isAfter(LocalDateTime.now()) }.getOrDefault(false)
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
                Row(
                    Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(EvColors.PrimaryDim)
                        .border(1.dp, EvColors.Primary.copy(0.35f), RoundedCornerShape(10.dp)),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.Bolt, null, tint = EvColors.Primary, modifier = Modifier.size(20.dp))
                }
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text("EV FINDER", style = MaterialTheme.typography.labelSmall, color = EvColors.Primary, letterSpacing = 1.sp)
                    Text("Bookings", style = MaterialTheme.typography.titleMedium, color = EvColors.OnBackground, fontWeight = FontWeight.Bold)
                }
                Icon(Icons.Outlined.Notifications, null, tint = EvColors.OnSurfaceVar, modifier = Modifier.size(24.dp))
                Spacer(Modifier.width(12.dp))
                Box(
                    Modifier.size(34.dp).clip(RoundedCornerShape(50)).background(EvColors.PrimaryDim),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.Person, null, tint = EvColors.Primary, modifier = Modifier.size(20.dp))
                }
            }
        }

        // ── Header ────────────────────────────────────────────────────────
        item {
            Column(Modifier.padding(horizontal = 16.dp)) {
                Text("Reservations", style = MaterialTheme.typography.headlineMedium, color = EvColors.OnBackground, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    LiveDot(activeSession != null)
                    Text(
                        if (activeSession != null) "1 Active charging session right now"
                        else "No active sessions",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (activeSession != null) EvColors.Primary else EvColors.OnSurfaceVar
                    )
                }
            }
        }

        // ── Filter tabs ───────────────────────────────────────────────────
        item {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                listOf("ALL" to "All", "UPCOMING" to "Upcoming", "COMPLETED" to "Past", "CANCELLED" to "Cancelled")
                    .forEach { (value, label) ->
                        val selected = state.filter == value
                        val count = state.bookings.count {
                            when (value) {
                                "UPCOMING"  -> it.status == "PENDING" || it.status == "CONFIRMED"
                                "COMPLETED" -> it.status == "COMPLETED"
                                "CANCELLED" -> it.status == "CANCELLED"
                                else        -> true
                            }
                        }
                        Box(
                            Modifier
                                .clip(RoundedCornerShape(50))
                                .background(if (selected) EvColors.Primary else Color.Transparent)
                                .clickable { viewModel.setFilter(value) }
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Text(
                                if (value != "ALL") "$label $count" else label,
                                color = if (selected) EvColors.OnPrimary else EvColors.OnSurfaceVar,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
                            )
                        }
                    }
            }
        }

        // ── States ────────────────────────────────────────────────────────
        when {
            state.loading -> item {
                Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = EvColors.Primary, strokeWidth = 2.dp)
                }
            }

            state.error != null -> item {
                Column(
                    Modifier.fillMaxWidth().padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Filled.WifiOff, null, tint = EvColors.OnSurfaceVar, modifier = Modifier.size(40.dp))
                    Spacer(Modifier.height(12.dp))
                    Text(state.error!!, color = EvColors.Error, style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(12.dp))
                    if (state.error!!.startsWith("Session expired"))
                        EvPrimaryButton("Log in again", onClick = onSessionExpired, modifier = Modifier.fillMaxWidth(0.6f))
                    else
                        TextButton(onClick = viewModel::load) { Text("Retry", color = EvColors.Primary) }
                }
            }

            visible.isEmpty() -> item {
                Box(Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Filled.CalendarMonth, null, tint = EvColors.OnSurfaceVar, modifier = Modifier.size(44.dp))
                        Spacer(Modifier.height(12.dp))
                        Text("No bookings here yet", style = MaterialTheme.typography.titleMedium, color = EvColors.OnBackground)
                        Text("Book a station from the Stations tab", color = EvColors.OnSurfaceVar, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            else -> {
                items(visible, key = { it.id }) { booking ->
                    BookingCard(
                        booking = booking,
                        cancelling = state.cancellingId == booking.id,
                        onCancel = { viewModel.cancel(booking.id) },
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                    )
                }
            }
        }

        // ── Autocharge pass footer ─────────────────────────────────────────
        item {
            Spacer(Modifier.height(12.dp))
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(EvColors.Surface)
                    .border(1.dp, EvColors.SurfaceBorder, RoundedCornerShape(14.dp))
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).background(EvColors.PrimaryDim),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.QrCode2, null, tint = EvColors.Primary, modifier = Modifier.size(20.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("Autocharge Pass Sync", style = MaterialTheme.typography.titleSmall, color = EvColors.OnBackground, fontWeight = FontWeight.SemiBold)
                    Text("Tap NFC or display code at terminal", style = MaterialTheme.typography.bodySmall, color = EvColors.OnSurfaceVar)
                }
                Row(
                    Modifier
                        .clip(RoundedCornerShape(50))
                        .background(EvColors.PrimaryDim)
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Open", color = EvColors.Primary, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold)
                    Icon(Icons.Filled.ChevronRight, null, tint = EvColors.Primary, modifier = Modifier.size(14.dp))
                }
            }
        }
    }
}

// ─── Premium booking card ─────────────────────────────────────────────────────
@Composable
private fun BookingCard(
    booking: BookingDto,
    cancelling: Boolean,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isActive = booking.status == "CONFIRMED" &&
        runCatching { LocalDateTime.parse(booking.startTime).isBefore(LocalDateTime.now()) &&
            LocalDateTime.parse(booking.endTime).isAfter(LocalDateTime.now()) }.getOrDefault(false)
    val isUpcoming = (booking.status == "PENDING" || booking.status == "CONFIRMED") &&
        runCatching { LocalDateTime.parse(booking.startTime).isAfter(LocalDateTime.now()) }.getOrDefault(false)
    val isConfirmed = booking.status == "CONFIRMED"

    val cardBg = if (isActive) EvColors.Surface else EvColors.Surface
    val borderColor = when {
        isActive    -> EvColors.Primary.copy(0.4f)
        isConfirmed -> EvColors.Primary.copy(0.15f)
        else        -> EvColors.SurfaceBorder
    }

    Column(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(cardBg)
            .border(1.dp, borderColor, RoundedCornerShape(18.dp))
    ) {
        // ── Status bar at top ──────────────────────────────────────────
        if (isActive) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(EvColors.PrimaryDim)
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    LiveDot()
                    Text("ACTIVE SESSION", style = MaterialTheme.typography.labelSmall, color = EvColors.Primary, fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp)
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Plugged In", style = MaterialTheme.typography.labelSmall, color = EvColors.OnSurfaceVar)
                    Text("•", color = EvColors.SurfaceBorder)
                    Text("Bay 04", style = MaterialTheme.typography.labelSmall, color = EvColors.OnSurface)
                }
            }
        }

        Column(Modifier.padding(16.dp)) {
            // ── Top row ──────────────────────────────────────────────────
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    if (!isActive) {
                        val (statusColor, statusLabel) = when (booking.status) {
                            "CONFIRMED" -> EvColors.Primary to "Confirmed & Ready"
                            "PENDING"   -> EvColors.Warning to "Pending payment"
                            "COMPLETED" -> EvColors.OnSurfaceVar to "Completed"
                            else        -> EvColors.Error to "Cancelled"
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            if (booking.status != "CANCELLED") LiveDot(booking.status == "CONFIRMED")
                            Text(statusLabel, style = MaterialTheme.typography.labelSmall, color = statusColor, fontWeight = FontWeight.SemiBold)
                        }
                        Spacer(Modifier.height(4.dp))
                    }
                    Text(
                        booking.stationName,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = EvColors.OnBackground
                    )
                    Spacer(Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Icons.Filled.ElectricCar, null, tint = EvColors.OnSurfaceVar, modifier = Modifier.size(14.dp))
                        val service = if (booking.serviceName == "BATTERY_SWAP") "Battery Swap" else "Charging"
                        Text(service, style = MaterialTheme.typography.bodySmall, color = EvColors.OnSurfaceVar)
                    }
                }
                Column(horizontalAlignment = Alignment.End) {
                    booking.amount?.let {
                        Text("Est. Cost", style = MaterialTheme.typography.labelSmall, color = EvColors.OnSurfaceVar)
                        Text(
                            "৳${it.toBigDecimal().stripTrailingZeros().toPlainString()}",
                            style = MaterialTheme.typography.titleLarge,
                            color = EvColors.OnBackground,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // ── Charging progress (active only) ───────────────────────
            if (isActive) {
                Spacer(Modifier.height(16.dp))
                ChargingProgressCard()
            }

            Spacer(Modifier.height(12.dp))

            // ── Time row ─────────────────────────────────────────────────
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(EvColors.SurfaceHigh)
                    .padding(10.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                InfoPair(
                    icon = Icons.Outlined.Schedule,
                    label = "SLOT TIME",
                    value = "${booking.startTime.take(10)} · ${booking.startTime.substring(11, 16)}–${booking.endTime.substring(11, 16)}",
                    modifier = Modifier.weight(1f)
                )
                if (isActive) {
                    InfoPair(
                        icon = Icons.Filled.Bolt,
                        label = "MAX SPEED",
                        value = "Ultra-Fast 35…",
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // ── Actions ──────────────────────────────────────────────────
            if (isUpcoming || isActive) {
                Spacer(Modifier.height(12.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    EvPrimaryButton(
                        text = if (isActive) "Get Directions" else "Get Directions",
                        onClick = {},
                        modifier = Modifier.weight(1f),
                        icon = Icons.Filled.Navigation
                    )
                    if (!isActive) {
                        EvOutlinedButton(
                            text = if (cancelling) "Cancelling…" else "Cancel / Modify",
                            onClick = { if (!cancelling) onCancel() },
                            modifier = Modifier.weight(1f)
                        )
                    } else {
                        Box(
                            Modifier.size(52.dp).clip(RoundedCornerShape(14.dp))
                                .background(EvColors.SurfaceHigh)
                                .border(1.dp, EvColors.SurfaceBorder, RoundedCornerShape(14.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Outlined.Info, null, tint = EvColors.OnSurfaceVar, modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }
        }
    }
}

// ─── Animated charging progress ───────────────────────────────────────────────
@Composable
private fun ChargingProgressCard() {
    val inf = rememberInfiniteTransition(label = "charge")
    val prog by inf.animateFloat(0.65f, 0.75f, infiniteRepeatable(tween(2000, easing = LinearEasing), RepeatMode.Reverse), label = "prog")

    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(EvColors.SurfaceHigh)
            .border(1.dp, EvColors.SurfaceBorder, RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Circular progress
        Box(Modifier.size(56.dp), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(
                progress = { prog },
                modifier = Modifier.fillMaxSize(),
                color = EvColors.Primary,
                trackColor = EvColors.SurfaceBorder,
                strokeWidth = 5.dp,
                strokeCap = StrokeCap.Round
            )
            Text("${(prog * 100).toInt()}%", style = MaterialTheme.typography.labelLarge, color = EvColors.OnBackground, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text("Charging at 350kW DC", style = MaterialTheme.typography.titleSmall, color = EvColors.OnBackground, fontWeight = FontWeight.SemiBold)
            Text("14 min left", style = MaterialTheme.typography.bodySmall, color = EvColors.Primary)
            Spacer(Modifier.height(4.dp))
            Text("Delivered 48.2 kWh of ~65.0 kWh", style = MaterialTheme.typography.labelSmall, color = EvColors.OnSurfaceVar)
        }
    }
}

// ─── Info pair ────────────────────────────────────────────────────────────────
@Composable
private fun InfoPair(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Icon(icon, null, tint = EvColors.OnSurfaceVar, modifier = Modifier.size(14.dp))
        Column {
            Text(label, style = MaterialTheme.typography.labelSmall, color = EvColors.OnSurfaceVar, letterSpacing = 0.5.sp)
            Text(value, style = MaterialTheme.typography.labelMedium, color = EvColors.OnSurface, fontWeight = FontWeight.SemiBold)
        }
    }
}
