@file:OptIn(ExperimentalLayoutApi::class)

package com.example.evfinder.feature.booking

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.evfinder.ui.theme.EvColors
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun BookingScreen(
    serviceId: String,
    onBookingCreated: (bookingId: String) -> Unit,
    onBack: () -> Unit = {}
) {
    val vm: BookingViewModel = viewModel(
        factory = viewModelFactory { initializer { BookingViewModel(serviceId) } }
    )
    val state by vm.uiState.collectAsState()

    LaunchedEffect(state.createdBooking?.id) {
        state.createdBooking?.let { onBookingCreated(it.id) }
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // back button — booking flow is always reversible
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(EvColors.Background)
                    .border(1.dp, EvColors.OutlineVariant, RoundedCornerShape(10.dp))
                    .clickable(onClick = onBack),
                contentAlignment = Alignment.Center
            ) {
                Text("←", style = MaterialTheme.typography.titleMedium, color = EvColors.OnSurface)
            }
            Spacer(Modifier.width(12.dp))
            Column {
                Text("Book a slot", style = MaterialTheme.typography.headlineSmall,
                    color = EvColors.OnBackground, fontWeight = FontWeight.Bold)
                Text("Vehicle → date → time", style = MaterialTheme.typography.labelSmall,
                    color = EvColors.OnSurfaceVar)
            }
        }

        state.error?.let {
            Spacer(Modifier.height(8.dp))
            Text(it, color = EvColors.Error, style = MaterialTheme.typography.bodySmall)
        }
        Spacer(Modifier.height(16.dp))

        // ---- 1. Vehicle ----
        SectionTitle("1. Select your EV")
        if (state.loading) {
            CircularProgressIndicator()
        } else if (state.vehicles.isEmpty()) {
            AddVehicleInline(onAdd = vm::addVehicle)
        } else {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                state.vehicles.forEach { v ->
                    FilterChip(
                        selected = state.selectedVehicleId == v.id,
                        onClick = { vm.selectVehicle(v.id) },
                        label = { Text("${v.registrationNo} · ${vehicleLabel(v.vehicleType)}") }
                    )
                }
            }
        }
        Spacer(Modifier.height(16.dp))

        // ---- 2. Date ----
        SectionTitle("2. Select date")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            state.dates.forEach { date ->
                FilterChip(
                    selected = state.selectedDate == date,
                    onClick = { vm.selectDate(date) },
                    label = { Text(dateLabel(date)) }
                )
            }
        }
        Spacer(Modifier.height(16.dp))

        // ---- 3. Time slot ----
        SectionTitle("3. Select time")
        when {
            state.slotsLoading -> CircularProgressIndicator()
            state.slots.isEmpty() -> Text(
                "No slots for this day (station closed or inactive).",
                color = EvColors.OnSurfaceVar
            )
            else -> FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                state.slots.forEach { slot ->
                    FilterChip(
                        selected = state.selectedSlot == slot,
                        onClick = { if (slot.bookable) vm.selectSlot(slot) },
                        enabled = slot.bookable,
                        label = {
                            Text("${slot.startTime.substring(11, 16)}\n${slot.available} left")
                        }
                    )
                }
            }
        }
        Spacer(Modifier.height(20.dp))

        // ---- 4. Summary + submit ----
        state.selectedSlot?.let { slot ->
            com.example.evfinder.ui.components.EvCard(Modifier.fillMaxWidth(), color = EvColors.Surface) {
                Column(Modifier.padding(16.dp)) {
                    Text("SUMMARY", style = MaterialTheme.typography.labelSmall,
                        color = EvColors.Primary, letterSpacing = 1.sp)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "${state.selectedDate.format(DateTimeFormatter.ofPattern("EEE, dd MMM"))} · " +
                            "${slot.startTime.substring(11, 16)} – ${slot.endTime.substring(11, 16)}",
                        color = EvColors.OnSurfaceVar
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
        }
        com.example.evfinder.ui.components.EvPrimaryButton(
            text = "Continue to Payment",
            onClick = vm::submitBooking,
            enabled = state.selectedVehicleId != null && state.selectedSlot != null && !state.submitting,
            loading = state.submitting,
            trailingIcon = Icons.AutoMirrored.Filled.ArrowForward,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text.uppercase(),
        style = MaterialTheme.typography.labelLarge,
        color = EvColors.Primary,
        letterSpacing = 1.sp,
        fontWeight = FontWeight.SemiBold
    )
    Spacer(Modifier.height(8.dp))
}

@Composable
private fun AddVehicleInline(onAdd: (vehicleType: String, regNo: String) -> Unit) {
    var type by remember { mutableStateOf("ELECTRIC_CAR") }
    var regNo by remember { mutableStateOf("") }
    com.example.evfinder.ui.components.EvCard(Modifier.fillMaxWidth(), color = EvColors.Surface) {
        Column(Modifier.padding(16.dp)) {
            Text("Add your EV to continue", style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(8.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("ELECTRIC_CAR" to "Car", "ELECTRIC_BIKE" to "Bike", "ELECTRIC_THREE_WHEELER" to "Rickshaw")
                    .forEach { (value, label) ->
                        FilterChip(selected = type == value, onClick = { type = value }, label = { Text(label) })
                    }
            }
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = regNo,
                onValueChange = { regNo = it },
                label = { Text("Registration no (e.g. DHA-11-2222)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            com.example.evfinder.ui.components.EvPrimaryButton(
                text = "Add vehicle",
                onClick = { onAdd(type, regNo.trim()) },
                enabled = regNo.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

private fun vehicleLabel(type: String) = when (type) {
    "ELECTRIC_CAR" -> "Car"
    "ELECTRIC_BIKE" -> "Bike"
    "ELECTRIC_THREE_WHEELER" -> "Rickshaw"
    else -> type
}

@RequiresApi(Build.VERSION_CODES.O)
private fun dateLabel(date: LocalDate): String {
    val today = LocalDate.now()
    return when (date) {
        today -> "Today"
        today.plusDays(1) -> "Tomorrow"
        else -> date.format(DateTimeFormatter.ofPattern("dd MMM"))
    }
}
