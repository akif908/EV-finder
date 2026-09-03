@file:OptIn(ExperimentalLayoutApi::class)

package com.example.evfinder.feature.booking

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.evfinder.core.model.BookingDto
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
                    // replace the cancelled booking in the list
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
}

@Composable
fun BookingsScreen(onSessionExpired: () -> Unit, viewModel: BookingsViewModel = viewModel()) {
    val state by viewModel.uiState.collectAsState()

    val visible = state.bookings.filter {
        when (state.filter) {
            "UPCOMING" -> it.status == "PENDING" || it.status == "CONFIRMED"
            "COMPLETED" -> it.status == "COMPLETED"
            "CANCELLED" -> it.status == "CANCELLED"
            else -> true
        }
    }

    Column(Modifier.fillMaxSize()) {
        Text(
            "My Bookings",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(start = 16.dp, top = 16.dp, end = 16.dp)
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            listOf("ALL" to "All", "UPCOMING" to "Upcoming", "COMPLETED" to "Completed", "CANCELLED" to "Cancelled")
                .forEach { (value, label) ->
                    FilterChip(selected = state.filter == value, onClick = { viewModel.setFilter(value) }, label = { Text(label) })
                }
        }

        when {
            state.loading -> Column(
                Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally
            ) { CircularProgressIndicator() }

            state.error != null -> Column(
                Modifier.fillMaxSize().padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally
            ) {
                Text(state.error!!, color = MaterialTheme.colorScheme.error)
                Spacer(Modifier.height(12.dp))
                if (state.error!!.startsWith("Session expired")) {
                    androidx.compose.material3.Button(onClick = onSessionExpired) { Text("Log in again") }
                } else {
                    androidx.compose.material3.TextButton(onClick = viewModel::load) { Text("Retry") }
                }
            }

            visible.isEmpty() -> Column(
                Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally
            ) {
                Text("No bookings here yet", style = MaterialTheme.typography.titleMedium)
                Text("Book a station from the Home tab", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            else -> LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(visible, key = { it.id }) { booking ->
                    BookingCard(
                        booking = booking,
                        cancelling = state.cancellingId == booking.id,
                        onCancel = { viewModel.cancel(booking.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun BookingCard(booking: BookingDto, cancelling: Boolean, onCancel: () -> Unit) {
    val (statusColor, statusLabel) = when (booking.status) {
        "CONFIRMED" -> MaterialTheme.colorScheme.primary to "Confirmed"
        "PENDING" -> MaterialTheme.colorScheme.tertiary to "Pending payment"
        "COMPLETED" -> MaterialTheme.colorScheme.onSurfaceVariant to "Completed"
        else -> MaterialTheme.colorScheme.error to "Cancelled"
    }

    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.padding(16.dp)) {
            Row {
                Text(
                    booking.stationName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
                Text(statusLabel, color = statusColor, style = MaterialTheme.typography.labelLarge)
            }
            Spacer(Modifier.height(6.dp))
            val service = if (booking.serviceName == "BATTERY_SWAP") "Battery Swap" else "Charging"
            Text("$service · ${booking.startTime.take(10)} · ${booking.startTime.substring(11, 16)}–${booking.endTime.substring(11, 16)}",
                color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
            booking.amount?.let {
                Spacer(Modifier.height(4.dp))
                Text("৳${it.toBigDecimal().stripTrailingZeros().toPlainString()}", fontWeight = FontWeight.SemiBold)
            }
            val upcoming = (booking.status == "PENDING" || booking.status == "CONFIRMED") &&
                runCatching { LocalDateTime.parse(booking.startTime).isAfter(LocalDateTime.now()) }.getOrDefault(false)
            if (upcoming) {
                Spacer(Modifier.height(10.dp))
                Button(onClick = onCancel, enabled = !cancelling) {
                    Text(if (cancelling) "Cancelling…" else "Cancel booking")
                }
            }
        }
    }
}
