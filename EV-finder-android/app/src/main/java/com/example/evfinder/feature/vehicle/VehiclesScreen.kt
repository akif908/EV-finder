@file:OptIn(ExperimentalLayoutApi::class)

package com.example.evfinder.feature.vehicle

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ElectricCar
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.TwoWheeler
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.evfinder.core.model.VehicleDto
import com.example.evfinder.feature.auth.EvTextField
import com.example.evfinder.ui.components.EvCard
import com.example.evfinder.ui.components.EvPrimaryButton
import com.example.evfinder.ui.components.EvTopBar
import com.example.evfinder.ui.components.StatusPill
import com.example.evfinder.ui.theme.EvColors

/** Vehicles tab: full CRUD over the user's EVs (list, add, edit, delete). */
@Composable
fun VehiclesScreen(viewModel: VehiclesViewModel = viewModel()) {
    val state by viewModel.uiState.collectAsState()

    Column(
        Modifier
            .fillMaxSize()
            .background(EvColors.Background)
    ) {
        EvTopBar(title = "My Vehicles", subtitle = "Manage the EVs you book with")

        when {
            state.loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = EvColors.Primary)
            }

            state.error != null && state.vehicles.isEmpty() -> Column(
                Modifier.fillMaxSize().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(Icons.Filled.ErrorOutline, null, tint = EvColors.OnSurfaceVar, modifier = Modifier.size(44.dp))
                Spacer(Modifier.height(12.dp))
                Text(state.error!!, color = EvColors.Error, style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(12.dp))
                if (state.error!!.startsWith("Session expired")) {
                    // caller handles session expiry via Profile logout; retry here
                    TextButton(onClick = viewModel::load) { Text("Retry", color = EvColors.Primary) }
                } else {
                    TextButton(onClick = viewModel::load) { Text("Retry", color = EvColors.Primary) }
                }
            }

            else -> Box(Modifier.fillMaxSize()) {
                LazyColumn(
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(state.vehicles, key = { it.id }) { vehicle ->
                        VehicleCard(
                            vehicle = vehicle,
                            deleting = state.deletingId == vehicle.id,
                            onEdit = { viewModel.openEdit(vehicle) },
                            onDelete = { viewModel.delete(vehicle.id) }
                        )
                    }
                    item {
                        Spacer(Modifier.height(64.dp)) // room behind the add button
                    }
                }

                EvPrimaryButton(
                    text = "Add Vehicle",
                    icon = Icons.Filled.Add,
                    onClick = viewModel::openAdd,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp)
                        .fillMaxWidth()
                )
            }
        }
    }

    if (state.showAdd) {
        VehicleDialog(
            editing = state.editing,
            saving = state.saving,
            onSave = { type, reg, manu, model, conn, kwh ->
                viewModel.save(type, reg, manu, model, conn, kwh)
            },
            onDismiss = viewModel::closeDialog
        )
    }
}

@Composable
private fun VehicleCard(vehicle: VehicleDto, deleting: Boolean, onEdit: () -> Unit, onDelete: () -> Unit) {
    EvCard {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(EvColors.PrimaryDim),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(vehicleIcon(vehicle.vehicleType), null,
                        tint = EvColors.Primary, modifier = Modifier.size(22.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        vehicleName(vehicle),
                        style = MaterialTheme.typography.titleMedium,
                        color = EvColors.OnBackground,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(vehicle.registrationNo, style = MaterialTheme.typography.bodySmall,
                        color = EvColors.OnSurfaceVar)
                }
                StatusPill(
                    label = vehicleLabel(vehicle.vehicleType),
                    isActive = true
                )
            }
            Spacer(Modifier.height(10.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                vehicle.connectorType?.let { InfoTag(it) }
                vehicle.batteryCapacityKwh?.let { InfoTag("${it.toBigDecimal().stripTrailingZeros().toPlainString()} kWh") }
            }
            Spacer(Modifier.height(8.dp))
            HorizontalDivider(color = EvColors.SurfaceBorder)
            Spacer(Modifier.height(6.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                IconButton(onClick = onEdit, enabled = !deleting) {
                    Icon(Icons.Filled.Edit, "Edit", tint = EvColors.OnSurfaceVar, modifier = Modifier.size(20.dp))
                }
                IconButton(onClick = onDelete, enabled = !deleting) {
                    if (deleting) {
                        CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = EvColors.Error)
                    } else {
                        Icon(Icons.Filled.Delete, "Delete", tint = EvColors.Error, modifier = Modifier.size(20.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoTag(text: String) {
    Box(
        Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(EvColors.SurfaceHigh)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(text, style = MaterialTheme.typography.labelSmall, color = EvColors.OnSurface)
    }
}

@Composable
private fun VehicleDialog(
    editing: VehicleDto?,
    saving: Boolean,
    onSave: (type: String, regNo: String, manufacturer: String, model: String, connector: String, batteryKwh: String) -> Unit,
    onDismiss: () -> Unit
) {
    var type by remember { mutableStateOf(editing?.vehicleType ?: "ELECTRIC_CAR") }
    var regNo by remember { mutableStateOf(editing?.registrationNo ?: "") }
    var manufacturer by remember { mutableStateOf(editing?.manufacturer ?: "") }
    var model by remember { mutableStateOf(editing?.model ?: "") }
    var connector by remember { mutableStateOf(editing?.connectorType ?: "CCS2") }
    var batteryKwh by remember {
        mutableStateOf(editing?.batteryCapacityKwh?.toBigDecimal()?.stripTrailingZeros()?.toPlainString() ?: "")
    }
    var localError by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = EvColors.Surface,
        titleContentColor = EvColors.OnBackground,
        textContentColor = EvColors.OnSurface,
        title = {
            Text(
                if (editing == null) "Add Vehicle" else "Edit Vehicle",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                // vehicle type
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        Triple("ELECTRIC_CAR", "Car", Icons.Filled.ElectricCar),
                        Triple("ELECTRIC_BIKE", "Bike", Icons.Filled.TwoWheeler)
                    ).forEach { (value, label, icon) ->
                        FilterChip(
                            selected = type == value,
                            onClick = { type = value },
                            label = { Text(label) },
                            leadingIcon = { Icon(icon, null, modifier = Modifier.size(16.dp)) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = EvColors.PrimaryDim,
                                selectedLabelColor = EvColors.Primary
                            )
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
                EvTextField(manufacturer, { manufacturer = it }, "Manufacturer (e.g. Nissan)",
                    leadingIcon = Icons.Filled.ElectricCar)
                Spacer(Modifier.height(8.dp))
                EvTextField(model, { model = it }, "Model (e.g. Leaf)")
                Spacer(Modifier.height(8.dp))
                EvTextField(regNo, { regNo = it }, "Registration no (e.g. DHA-11-2222)")
                Spacer(Modifier.height(8.dp))
                EvTextField(connector, { connector = it }, "Connector (CCS2 / Type2 / GB/T)")
                Spacer(Modifier.height(8.dp))
                EvTextField(batteryKwh, { batteryKwh = it }, "Battery capacity kWh (optional)",
                    keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal)
                localError?.let {
                    Spacer(Modifier.height(8.dp))
                    Text(it, color = EvColors.Error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            EvPrimaryButton(
                text = if (saving) "Saving…" else if (editing == null) "Add" else "Save",
                onClick = {
                    if (regNo.isBlank()) {
                        localError = "Registration number is required"
                    } else {
                        localError = null
                        onSave(type, regNo.trim(), manufacturer.trim(), model.trim(), connector.trim(), batteryKwh.trim())
                    }
                },
                enabled = !saving
            )
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = EvColors.OnSurfaceVar) }
        }
    )
}

private fun vehicleName(v: VehicleDto): String =
    listOfNotNull(v.manufacturer, v.model).joinToString(" ").ifBlank { vehicleLabel(v.vehicleType) }

private fun vehicleLabel(type: String) = when (type) {
    "ELECTRIC_CAR" -> "Car"
    "ELECTRIC_BIKE" -> "Bike"
    "ELECTRIC_THREE_WHEELER" -> "Rickshaw"
    else -> type
}

private fun vehicleIcon(type: String): ImageVector = when (type) {
    "ELECTRIC_BIKE" -> Icons.Filled.TwoWheeler
    else -> Icons.Filled.ElectricCar
}
