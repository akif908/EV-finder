@file:OptIn(ExperimentalLayoutApi::class)

package com.example.evfinder.feature.booking

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.evfinder.core.model.BookingDto
import com.example.evfinder.ui.components.EvCard
import com.example.evfinder.ui.components.EvPrimaryButton
import com.example.evfinder.ui.components.EvSegmentedTabs
import com.example.evfinder.ui.components.EvSpecTile
import com.example.evfinder.ui.components.EvStatusChip
import com.example.evfinder.ui.components.EvTopBar
import com.example.evfinder.ui.components.EvWideButton
import com.example.evfinder.ui.components.LiveDot
import com.example.evfinder.ui.theme.EvColors
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.LocalDateTime

data class BookingsUiState(
    val loading: Boolean = true,
    val bookings: List<BookingDto> = emptyList(),
    val filter: String = "UPCOMING",
    val error: String? = null,
    val cancellingId: String? = null,
    // review dialog
    val reviewBooking: BookingDto? = null,
    val reviewRating: Int = 0,
    val reviewComment: String = "",
    val submittingReview: Boolean = false
)

class BookingsViewModel : ViewModel() {

    private val repository = BookingRepository()

    private val _uiState = MutableStateFlow(BookingsUiState())
    val uiState: StateFlow<BookingsUiState> = _uiState

    init { load() }

    fun load() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(loading = true, error = null)
            repository.myBookings().fold(
                onSuccess = { b -> _uiState.value = _uiState.value.copy(loading = false, bookings = b) },
                onFailure = { e -> _uiState.value = _uiState.value.copy(loading = false, error = e.message) }
            )
        }
    }

    fun setFilter(filter: String) {
        _uiState.value = _uiState.value.copy(filter = filter)
    }

    fun cancel(id: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(cancellingId = id)
            repository.cancelBooking(id).fold(
                onSuccess = { updated ->
                    _uiState.value = _uiState.value.copy(cancellingId = null)
                    _uiState.value = _uiState.value.copy(
                        bookings = _uiState.value.bookings.map { if (it.id == updated.id) updated else it }
                    )
                },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(cancellingId = null, error = e.message)
                }
            )
        }
    }

    // ---- reviews ----

    fun openReview(booking: BookingDto) {
        _uiState.value = _uiState.value.copy(reviewBooking = booking, reviewRating = 0, reviewComment = "")
    }

    fun closeReview() {
        _uiState.value = _uiState.value.copy(reviewBooking = null, submittingReview = false)
    }

    fun setRating(rating: Int) { _uiState.value = _uiState.value.copy(reviewRating = rating) }
    fun setComment(comment: String) { _uiState.value = _uiState.value.copy(reviewComment = comment) }

    fun submitReview() {
        val s = _uiState.value
        val booking = s.reviewBooking ?: return
        if (s.reviewRating !in 1..5) return
        viewModelScope.launch {
            _uiState.value = s.copy(submittingReview = true, error = null)
            repository.submitReview(booking.id, s.reviewRating, s.reviewComment).fold(
                onSuccess = { _uiState.value = _uiState.value.copy(reviewBooking = null, submittingReview = false) },
                onFailure = { e -> _uiState.value = _uiState.value.copy(submittingReview = false, error = e.message) }
            )
        }
    }
}

@Composable
fun BookingsScreen(onSessionExpired: () -> Unit, viewModel: BookingsViewModel = viewModel()) {
    val state by viewModel.uiState.collectAsState()

    val visible = state.bookings.filter {
        when (state.filter) {
            "UPCOMING" -> it.status == "PENDING" || it.status == "CONFIRMED"
            "COMPLETED" -> it.status == "COMPLETED"
            else -> it.status == "CANCELLED"
        }
    }

    Column(Modifier.fillMaxSize().background(EvColors.Background)) {
        EvTopBar(title = "Bookings", subtitle = "Manage your charging sessions")

        // segmented tabs with live counts (mockup)
        val upcomingCount = state.bookings.count { it.status == "PENDING" || it.status == "CONFIRMED" }
        val completedCount = state.bookings.count { it.status == "COMPLETED" }
        val cancelledCount = state.bookings.count { it.status == "CANCELLED" }
        val tabs = listOf("Upcoming", "Past", "Cancelled")
        val values = listOf("UPCOMING", "COMPLETED", "CANCELLED")
        EvSegmentedTabs(
            options = tabs,
            selectedIndex = values.indexOf(state.filter).coerceAtLeast(0),
            counts = listOf(upcomingCount, completedCount, cancelledCount),
            onSelect = { viewModel.setFilter(values[it]) },
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
        )

        when {
            state.loading -> Box(
                Modifier.fillMaxSize().background(EvColors.Background),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator(color = EvColors.Primary) }

            state.error != null -> Column(
                Modifier.fillMaxSize().background(EvColors.Background).padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(Icons.Filled.ErrorOutline, null,
                    tint = EvColors.OnSurfaceVar, modifier = Modifier.size(44.dp))
                Spacer(Modifier.height(12.dp))
                Text(state.error!!, color = EvColors.Error,
                    style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(12.dp))
                if (state.error!!.startsWith("Session expired")) {
                    EvPrimaryButton("Log in again", onSessionExpired)
                } else {
                    TextButton(onClick = viewModel::load) { Text("Retry", color = EvColors.Primary) }
                }
            }

            visible.isEmpty() -> Column(
                Modifier.fillMaxSize().background(EvColors.Background),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(EvColors.Surface),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.AccessTime, null,
                        tint = EvColors.OnSurfaceVar, modifier = Modifier.size(26.dp))
                }
                Spacer(Modifier.height(14.dp))
                Text(
                    when (state.filter) {
                        "COMPLETED" -> "No completed sessions yet"
                        "CANCELLED" -> "No cancelled bookings"
                        else -> "No upcoming bookings"
                    },
                    style = MaterialTheme.typography.titleMedium, color = EvColors.OnBackground
                )
                Text(
                    "Book a station from the Stations tab",
                    style = MaterialTheme.typography.bodySmall,
                    color = EvColors.OnSurfaceVar
                )
            }

            else -> LazyColumn(
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(visible, key = { it.id }) { booking ->
                    BookingCard(
                        booking = booking,
                        cancelling = state.cancellingId == booking.id,
                        onCancel = { viewModel.cancel(booking.id) },
                        onReview = { viewModel.openReview(booking) }
                    )
                }
            }
        }
    }

    // ---- review dialog ----
    state.reviewBooking?.let { booking ->
        AlertDialog(
            onDismissRequest = viewModel::closeReview,
            containerColor = EvColors.Surface,
            title = {
                Text("Rate ${booking.stationName}", color = EvColors.OnBackground,
                    fontWeight = FontWeight.Bold)
            },
            text = {
                Column {
                    Text("How was your session?",
                        style = MaterialTheme.typography.bodySmall,
                        color = EvColors.OnSurfaceVar)
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth()) {
                        (1..5).forEach { star ->
                            Text(
                                if (star <= state.reviewRating) "★" else "☆",
                                color = if (star <= state.reviewRating) EvColors.Primary
                                        else EvColors.SurfaceHighest,
                                style = MaterialTheme.typography.headlineMedium,
                                modifier = Modifier
                                    .padding(4.dp)
                                    .clickable { viewModel.setRating(star) }
                            )
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = state.reviewComment,
                        onValueChange = viewModel::setComment,
                        placeholder = { Text("Optional comment",
                            color = EvColors.OnSurfaceVar.copy(alpha = 0.5f)) },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 3,
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = EvColors.Primary,
                            unfocusedBorderColor = EvColors.OutlineVariant,
                            focusedContainerColor = EvColors.InputBackground,
                            unfocusedContainerColor = EvColors.InputBackground,
                            cursorColor = EvColors.Primary,
                            focusedTextColor = EvColors.OnSurface,
                            unfocusedTextColor = EvColors.OnSurface
                        )
                    )
                    state.error?.let {
                        Spacer(Modifier.height(8.dp))
                        Text(it, color = EvColors.Error,
                            style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            confirmButton = {
                EvPrimaryButton(
                    text = if (state.submittingReview) "Submitting…" else "Submit review",
                    onClick = viewModel::submitReview,
                    enabled = state.reviewRating in 1..5 && !state.submittingReview
                )
            },
            dismissButton = {
                TextButton(onClick = viewModel::closeReview) {
                    Text("Cancel", color = EvColors.OnSurfaceVar)
                }
            }
        )
    }
}

@Composable
private fun BookingCard(booking: BookingDto, cancelling: Boolean, onCancel: () -> Unit, onReview: () -> Unit) {
    val statusChip = when (booking.status) {
        "CONFIRMED" -> EvStatusChip(label = "Confirmed", icon = Icons.Filled.Check,
            accent = EvColors.Primary, pulsingDot = true)
        "PENDING" -> EvStatusChip(label = "Pending payment", accent = EvColors.Warning,
            pulsingDot = true)
        "COMPLETED" -> EvStatusChip(label = "Completed", icon = Icons.Filled.Check,
            accent = EvColors.Secondary)
        else -> EvStatusChip(label = "Cancelled", accent = EvColors.Error)
    }

    val past = runCatching { LocalDateTime.parse(booking.endTime).isBefore(LocalDateTime.now()) }
        .getOrDefault(false)
    val upcoming = (booking.status == "PENDING" || booking.status == "CONFIRMED") &&
        runCatching { LocalDateTime.parse(booking.startTime).isAfter(LocalDateTime.now()) }
            .getOrDefault(false)
    val reviewable = (booking.status == "COMPLETED") ||
        (booking.status == "CONFIRMED" && past)

    EvCard(Modifier.fillMaxWidth(), color = EvColors.Surface) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    booking.stationName,
                    style = MaterialTheme.typography.titleMedium,
                    color = EvColors.OnBackground, fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f), maxLines = 1
                )
                statusChip
            }

            Spacer(Modifier.height(10.dp))

            // spec strip (mockup): date / time window / price
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                EvSpecTile(
                    icon = Icons.Filled.AccessTime,
                    value = booking.startTime.substring(11, 16),
                    caption = booking.endTime.substring(11, 16),
                    modifier = Modifier.weight(1f)
                )
                EvSpecTile(
                    icon = Icons.Filled.Bolt,
                    value = if (booking.serviceName == "BATTERY_SWAP") "Swap" else "Charging",
                    caption = booking.startTime.take(10),
                    accent = EvColors.Secondary,
                    modifier = Modifier.weight(1f)
                )
                EvSpecTile(
                    icon = Icons.Filled.Star,
                    value = booking.amount
                        ?.let { "৳${it.toBigDecimal().stripTrailingZeros().toPlainString()}" } ?: "—",
                    caption = if (booking.status == "CONFIRMED") "Paid" else "Est.",
                    modifier = Modifier.weight(1f)
                )
            }

            val cancelled = booking.status == "CANCELLED"
            if (upcoming) {
                Spacer(Modifier.height(14.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    EvWideButton(
                        text = if (cancelling) "Cancelling…" else "Cancel booking",
                        icon = Icons.Filled.NearMe,
                        container = EvColors.SurfaceHigh,
                        contentColor = EvColors.OnSurface,
                        enabled = !cancelling,
                        onClick = onCancel,
                        modifier = Modifier.weight(1f)
                    )
                }
            } else if (reviewable && !cancelled) {
                Spacer(Modifier.height(14.dp))
                EvWideButton(
                    text = "Rate this station",
                    icon = Icons.Filled.Star,
                    container = EvColors.Primary.copy(alpha = 0.15f),
                    contentColor = EvColors.Primary,
                    onClick = onReview,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

