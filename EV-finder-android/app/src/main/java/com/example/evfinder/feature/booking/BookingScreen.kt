@file:OptIn(ExperimentalLayoutApi::class)

package com.example.evfinder.feature.booking

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.evfinder.ui.components.*
import com.example.evfinder.ui.theme.EvColors
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun BookingScreen(
    serviceId: String,
    onBack: () -> Unit,
    onBookingCreated: (bookingId: String) -> Unit
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
            .background(EvColors.Background)
            .verticalScroll(rememberScrollState())
    ) {
        // ── App bar ───────────────────────────────────────────────────────
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
                    .clickable(enabled = !state.submitting, onClick = onBack),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Filled.ArrowBack,
                    null,
                    tint = if (state.submitting) EvColors.OnSurface.copy(0.4f) else EvColors.OnSurface,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(Modifier.width(12.dp))
            Text("Book a Slot", style = MaterialTheme.typography.titleLarge, color = EvColors.OnBackground, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
        }

        // ── Step progress ─────────────────────────────────────────────────
        Column(Modifier.padding(horizontal = 16.dp)) {
            val currentStep = when {
                state.selectedVehicleId == null -> 1
                state.selectedDate == LocalDate.now() && state.selectedSlot == null -> 2
                state.selectedSlot == null -> 3
                else -> 4
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                repeat(4) { idx ->
                    val step = idx + 1
                    val done = step < currentStep
                    val active = step == currentStep
                    Box(
                        Modifier
                            .weight(1f)
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(when { done -> EvColors.Primary; active -> EvColors.Primary.copy(0.6f); else -> EvColors.SurfaceBorder })
                    )
                }
            }
            Spacer(Modifier.height(6.dp))
            Text("Step $currentStep of 4", style = MaterialTheme.typography.labelSmall, color = EvColors.OnSurfaceVar)
            Spacer(Modifier.height(16.dp))

            // ── Error ─────────────────────────────────────────────────────
            state.error?.let {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(EvColors.Error.copy(0.1f))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.Warning, null, tint = EvColors.Error, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(it, color = EvColors.Error, style = MaterialTheme.typography.bodySmall)
                }
                Spacer(Modifier.height(12.dp))
            }

            // ── 1. Vehicle ────────────────────────────────────────────────
            SectionStep(number = "1", title = "Select your EV")
            Spacer(Modifier.height(10.dp))
            if (state.loading) {
                Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = EvColors.Primary, strokeWidth = 2.dp)
                }
            } else if (state.vehicles.isEmpty()) {
                AddVehicleInline(onAdd = vm::addVehicle)
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    state.vehicles.forEach { v ->
                        val selected = state.selectedVehicleId == v.id
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (selected) EvColors.PrimaryDim else EvColors.SurfaceHigh)
                                .border(
                                    1.dp,
                                    if (selected) EvColors.Primary else EvColors.SurfaceBorder,
                                    RoundedCornerShape(12.dp)
                                )
                                .clickable { vm.selectVehicle(v.id) }
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Filled.ElectricCar,
                                null,
                                tint = if (selected) EvColors.Primary else EvColors.OnSurfaceVar,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    v.registrationNo,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = if (selected) EvColors.OnBackground else EvColors.OnSurface,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(vehicleLabel(v.vehicleType), style = MaterialTheme.typography.bodySmall, color = EvColors.OnSurfaceVar)
                            }
                            if (selected) {
                                Icon(Icons.Filled.CheckCircle, null, tint = EvColors.Primary, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            // ── 2. Date ───────────────────────────────────────────────────
            SectionStep(number = "2", title = "Select date")
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                state.dates.forEach { date ->
                    val selected = state.selectedDate == date
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (selected) EvColors.Primary else EvColors.SurfaceHigh)
                            .border(1.dp, if (selected) EvColors.Primary else EvColors.SurfaceBorder, RoundedCornerShape(12.dp))
                            .clickable { vm.selectDate(date) }
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        Text(
                            dateLabel(date),
                            color = if (selected) EvColors.OnPrimary else EvColors.OnSurface,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
                        )
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            // ── 3. Time slot ──────────────────────────────────────────────
            SectionStep(number = "3", title = "Select time")
            Spacer(Modifier.height(10.dp))
            when {
                state.slotsLoading -> Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = EvColors.Primary, strokeWidth = 2.dp)
                }
                state.slots.isEmpty() -> Column(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(EvColors.SurfaceHigh)
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Filled.EventBusy, null, tint = EvColors.OnSurfaceVar, modifier = Modifier.size(28.dp))
                    Spacer(Modifier.height(8.dp))
                    Text("No slots for this day (station closed or inactive).", color = EvColors.OnSurfaceVar, style = MaterialTheme.typography.bodySmall)
                }
                else -> {
                    // 3-per-row grid
                    val rows = state.slots.chunked(3)
                    rows.forEach { row ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            row.forEach { slot ->
                                val selected = state.selectedSlot == slot
                                val available = slot.bookable
                                Box(
                                    Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(
                                            when {
                                                selected   -> EvColors.Primary
                                                !available -> EvColors.SurfaceBorder.copy(0.3f)
                                                else       -> EvColors.SurfaceHigh
                                            }
                                        )
                                        .border(
                                            1.dp,
                                            when { selected -> EvColors.Primary; !available -> EvColors.SurfaceBorder.copy(0.3f); else -> EvColors.SurfaceBorder },
                                            RoundedCornerShape(12.dp)
                                        )
                                        .clickable(enabled = available) { vm.selectSlot(slot) }
                                        .padding(vertical = 12.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            slot.startTime.substring(11, 16),
                                            style = MaterialTheme.typography.labelLarge,
                                            color = when { selected -> EvColors.OnPrimary; !available -> EvColors.OnSurfaceVar.copy(0.4f); else -> EvColors.OnSurface },
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Text(
                                            "${slot.available} left",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = when { selected -> EvColors.OnPrimary.copy(0.8f); !available -> EvColors.OnSurfaceVar.copy(0.3f); else -> EvColors.Primary }
                                        )
                                    }
                                }
                            }
                            // fill empty cells
                            repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                        }
                        Spacer(Modifier.height(8.dp))
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            // ── 4. Summary card ───────────────────────────────────────────
            state.selectedSlot?.let { slot ->
                Column(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(EvColors.PrimaryDim)
                        .border(1.dp, EvColors.Primary.copy(0.3f), RoundedCornerShape(14.dp))
                        .padding(16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.CheckCircle, null, tint = EvColors.Primary, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Booking Summary", style = MaterialTheme.typography.titleSmall, color = EvColors.OnBackground, fontWeight = FontWeight.SemiBold)
                    }
                    Spacer(Modifier.height(10.dp))
                    HorizontalDivider(color = EvColors.Primary.copy(0.2f))
                    Spacer(Modifier.height(10.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Date", style = MaterialTheme.typography.bodySmall, color = EvColors.OnSurfaceVar)
                        Text(state.selectedDate.format(DateTimeFormatter.ofPattern("EEE, dd MMM")), style = MaterialTheme.typography.bodySmall, color = EvColors.OnBackground, fontWeight = FontWeight.SemiBold)
                    }
                    Spacer(Modifier.height(6.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Time", style = MaterialTheme.typography.bodySmall, color = EvColors.OnSurfaceVar)
                        Text("${slot.startTime.substring(11, 16)} – ${slot.endTime.substring(11, 16)}", style = MaterialTheme.typography.bodySmall, color = EvColors.OnBackground, fontWeight = FontWeight.SemiBold)
                    }
                }
                Spacer(Modifier.height(14.dp))
            }

            EvPrimaryButton(
                text = "Continue to Payment",
                onClick = vm::submitBooking,
                modifier = Modifier.fillMaxWidth(),
                enabled = state.selectedVehicleId != null && state.selectedSlot != null && !state.submitting,
                loading = state.submitting
            )

            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun SectionStep(number: String, title: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(
            Modifier
                .size(24.dp)
                .clip(RoundedCornerShape(50))
                .background(EvColors.PrimaryDim)
                .border(1.dp, EvColors.Primary, RoundedCornerShape(50)),
            contentAlignment = Alignment.Center
        ) {
            Text(number, style = MaterialTheme.typography.labelSmall, color = EvColors.Primary, fontWeight = FontWeight.Bold)
        }
        Text(title, style = MaterialTheme.typography.titleMedium, color = EvColors.OnBackground, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun AddVehicleInline(onAdd: (vehicleType: String, regNo: String) -> Unit) {
    var type by remember { mutableStateOf("ELECTRIC_CAR") }
    var regNo by remember { mutableStateOf("") }

    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(EvColors.Surface)
            .border(1.dp, EvColors.SurfaceBorder, RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.AddCircle, null, tint = EvColors.Primary, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Add your EV to continue", style = MaterialTheme.typography.titleSmall, color = EvColors.OnBackground, fontWeight = FontWeight.SemiBold)
        }
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("ELECTRIC_CAR" to "Car", "ELECTRIC_BIKE" to "Bike", "ELECTRIC_THREE_WHEELER" to "Rickshaw")
                .forEach { (value, label) ->
                    val sel = type == value
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(50))
                            .background(if (sel) EvColors.PrimaryDim else EvColors.SurfaceHigh)
                            .border(1.dp, if (sel) EvColors.Primary else EvColors.SurfaceBorder, RoundedCornerShape(50))
                            .clickable { type = value }
                            .padding(horizontal = 12.dp, vertical = 7.dp)
                    ) {
                        Text(label, color = if (sel) EvColors.Primary else EvColors.OnSurfaceVar, style = MaterialTheme.typography.labelSmall)
                    }
                }
        }
        Spacer(Modifier.height(10.dp))
        com.example.evfinder.feature.auth.EvTextField(
            value = regNo,
            onValueChange = { regNo = it },
            placeholder = "DHA-11-2222",
            leadingIcon = Icons.Outlined.DirectionsCar
        )
        Spacer(Modifier.height(10.dp))
        EvPrimaryButton(
            text = "Add Vehicle",
            onClick = { onAdd(type, regNo.trim()) },
            modifier = Modifier.fillMaxWidth(),
            enabled = regNo.isNotBlank()
        )
    }
}

private fun vehicleLabel(type: String) = when (type) {
    "ELECTRIC_CAR"           -> "Car"
    "ELECTRIC_BIKE"          -> "Bike"
    "ELECTRIC_THREE_WHEELER" -> "Rickshaw"
    else                     -> type
}

private fun dateLabel(date: LocalDate): String {
    val today = LocalDate.now()
    return when (date) {
        today            -> "Today"
        today.plusDays(1)-> "Tomorrow"
        else             -> date.format(DateTimeFormatter.ofPattern("dd MMM"))
    }
}
