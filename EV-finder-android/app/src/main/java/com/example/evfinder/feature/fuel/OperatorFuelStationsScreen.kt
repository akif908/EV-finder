package com.example.evfinder.feature.fuel

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.evfinder.core.model.FuelInventoryDto
import com.example.evfinder.core.model.FuelStationDto
import com.example.evfinder.feature.auth.EvTextField
import com.example.evfinder.feature.operator.LocationPickerOverlay
import com.example.evfinder.ui.components.EvCard
import com.example.evfinder.ui.components.EvOutlinedButton
import com.example.evfinder.ui.components.EvPrimaryButton
import com.example.evfinder.ui.components.EvTopBar
import com.example.evfinder.ui.components.StatusPill
import com.example.evfinder.ui.theme.EvColors
import kotlinx.coroutines.launch

/**
 * Operator fuel station management: create/edit/delete fuel stations,
 * open/close, and per-fuel-type queue / remaining liters / BDT price updates.
 * Separate from the EV stations screen — fuel stations have no booking.
 */
@Composable
fun OperatorFuelStationsScreen(onSessionExpired: () -> Unit) {
    val vm: OperatorFuelViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
    val state by vm.uiState.collectAsState()
    val scope = rememberCoroutineScope()

    Box(Modifier.fillMaxSize().background(EvColors.Background)) {
        Column(Modifier.fillMaxSize()) {
            EvTopBar(title = "Fuel Stations", subtitle = "Queues, stock and prices — no booking")

            when {
                state.loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = EvColors.Primary)
                }

                state.error != null && state.stations.isEmpty() -> Column(
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

                else -> LazyColumn(
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 88.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (state.stations.isEmpty()) {
                        item {
                            Text(
                                "No fuel stations yet — add your first one.",
                                style = MaterialTheme.typography.bodySmall, color = EvColors.OnSurfaceVar
                            )
                        }
                    }
                    items(state.stations, key = { it.id }) { station ->
                        OperatorFuelStationCard(
                            station = station,
                            onEdit = { vm.openEdit(station) },
                            onToggleOpen = { vm.toggleOpen(station) },
                            onDelete = { vm.askDelete(station) },
                            onEditFuel = { vm.openFuelDialog(station.id, it) },
                            onRemoveFuel = { vm.removeFuel(station.id, it.fuelType) },
                            onAddFuel = { vm.openFuelDialog(station.id, null) }
                        )
                    }
                }
            }
        }

        EvPrimaryButton(
            text = "Add Fuel Station",
            icon = Icons.Filled.Add,
            onClick = vm::openCreate,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp)
                .fillMaxWidth()
        )
    }

    // Uber-style map picker for the station location (shared with EV operator flow)
    if (state.pickingLocation) {
        LocationPickerOverlay(
            onConfirm = vm::confirmLocation,
            onCancel = vm::cancelLocationPicker
        )
    }

    if (state.stationForm != null || state.stationFormIsNew) {
        FuelStationFormDialog(
            editing = state.stationForm,
            pendingLat = state.pendingLat,
            pendingLng = state.pendingLng,
            saving = state.saving,
            onSave = vm::saveStation,
            onDismiss = vm::closeForms
        )
    }

    state.fuelDialogStationId?.let { stationId ->
        FuelInventoryDialog(
            station = state.stations.firstOrNull { it.id == stationId },
            editing = state.fuelForm,
            saving = state.saving,
            onSave = { type, queue, liters, price ->
                scope.launch { vm.saveFuel(stationId, type, queue, liters, price) }
            },
            onDismiss = vm::closeForms
        )
    }

    state.confirmDelete?.let { station ->
        AlertDialog(
            onDismissRequest = vm::dismissDelete,
            containerColor = EvColors.Surface,
            title = { Text("Delete fuel station?", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "\"${station.name}\" and all its fuel info will be removed permanently.",
                    style = MaterialTheme.typography.bodyMedium, color = EvColors.OnSurface
                )
            },
            confirmButton = {
                TextButton(onClick = vm::deleteConfirmed) {
                    Text("Delete", color = EvColors.Error)
                }
            },
            dismissButton = {
                TextButton(onClick = vm::dismissDelete) { Text("Cancel", color = EvColors.OnSurfaceVar) }
            }
        )
    }
}

@Composable
private fun OperatorFuelStationCard(
    station: FuelStationDto,
    onEdit: () -> Unit,
    onToggleOpen: () -> Unit,
    onDelete: () -> Unit,
    onEditFuel: (FuelInventoryDto) -> Unit,
    onRemoveFuel: (FuelInventoryDto) -> Unit,
    onAddFuel: () -> Unit
) {
    EvCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(station.name, style = MaterialTheme.typography.titleMedium,
                        color = EvColors.OnBackground, fontWeight = FontWeight.SemiBold)
                    station.address?.let {
                        Text(it, style = MaterialTheme.typography.bodySmall, color = EvColors.OnSurfaceVar)
                    }
                }
                StatusPill(label = if (station.isOpen) "Open" else "Closed", isActive = station.isOpen)
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ActionChipFuel("Edit", Icons.Filled.Edit, onEdit)
                ActionChipFuel(if (station.isOpen) "Close" else "Reopen", Icons.Filled.LocalGasStation, onToggleOpen)
                ActionChipFuel("Delete", Icons.Filled.Delete, onDelete)
            }
            Spacer(Modifier.height(12.dp))
            HorizontalDivider(color = EvColors.SurfaceBorder)
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Fuel types", style = MaterialTheme.typography.labelLarge, color = EvColors.OnSurfaceVar,
                    modifier = Modifier.weight(1f))
                Text("+ add", color = EvColors.Primary, style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable(onClick = onAddFuel).padding(4.dp))
            }
            station.inventories.forEach { inv ->
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.LocalGasStation, null, tint = EvColors.Primary, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Column(Modifier.weight(1f)) {
                        Text(fuelLabel(inv.fuelType), style = MaterialTheme.typography.bodyMedium, color = EvColors.OnSurface)
                        Text(
                            "Queue ${inv.queueCount} · ${liters(inv.remainingLiters)} L · ৳${bdt(inv.pricePerLiter)}/L",
                            style = MaterialTheme.typography.labelSmall, color = EvColors.OnSurfaceVar
                        )
                    }
                    IconButton(onClick = { onEditFuel(inv) }, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Filled.Edit, "Edit ${fuelLabel(inv.fuelType)}", tint = EvColors.OnSurfaceVar, modifier = Modifier.size(16.dp))
                    }
                    IconButton(onClick = { onRemoveFuel(inv) }, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Filled.Delete, "Remove ${fuelLabel(inv.fuelType)}", tint = EvColors.Error, modifier = Modifier.size(16.dp))
                    }
                }
            }
            if (station.inventories.isEmpty()) {
                Text("No fuel types yet — add LPG/Diesel/Octane/Petrol info.",
                    style = MaterialTheme.typography.bodySmall, color = EvColors.OnSurfaceVar)
            }
        }
    }
}

@Composable
private fun ActionChipFuel(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Row(
        Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(EvColors.SurfaceHigh)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(icon, null, tint = EvColors.OnSurfaceVar, modifier = Modifier.size(14.dp))
        Text(label, style = MaterialTheme.typography.labelMedium, color = EvColors.OnSurface)
    }
}

// ─── Create / edit fuel station dialog (coordinates from the map picker) ─────
@Composable
private fun FuelStationFormDialog(
    editing: FuelStationDto?,
    pendingLat: Double?,
    pendingLng: Double?,
    saving: Boolean,
    onSave: (name: String, desc: String, address: String, isOpen: Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(editing?.name ?: "") }
    var desc by remember { mutableStateOf(editing?.description ?: "") }
    var address by remember { mutableStateOf(editing?.address ?: "") }
    var isOpen by remember { mutableStateOf(editing?.isOpen ?: true) }
    var error by remember { mutableStateOf<String?>(null) }

    val latText = pendingLat?.let { "%.5f".format(it) } ?: editing?.latitude?.let { "%.5f".format(it) }
    val lngText = pendingLng?.let { "%.5f".format(it) } ?: editing?.longitude?.let { "%.5f".format(it) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = EvColors.Surface,
        title = { Text(if (editing == null) "New Fuel Station" else "Edit Fuel Station", fontWeight = FontWeight.Bold) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FuelInfoTag("📍 lat: $latText")
                    FuelInfoTag("📍 lng: $lngText")
                }
                Spacer(Modifier.height(12.dp))
                EvTextField(name, { name = it }, "Station name *")
                Spacer(Modifier.height(8.dp))
                EvTextField(address, { address = it }, "Address")
                Spacer(Modifier.height(8.dp))
                EvTextField(desc, { desc = it }, "Description")
                Spacer(Modifier.height(8.dp))
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).clickable { isOpen = !isOpen }.padding(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Station is open", style = MaterialTheme.typography.bodyMedium,
                        color = EvColors.OnSurface, modifier = Modifier.weight(1f))
                    Switch(checked = isOpen, onCheckedChange = { isOpen = it })
                }
                error?.let {
                    Spacer(Modifier.height(8.dp))
                    Text(it, color = EvColors.Error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            EvPrimaryButton(
                text = if (saving) "Saving…" else "Save",
                onClick = {
                    if (name.isBlank()) error = "Station name is required"
                    else {
                        error = null
                        onSave(name, desc, address, isOpen)
                    }
                },
                enabled = !saving
            )
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = EvColors.OnSurfaceVar) } }
    )
}

// ─── Add / edit fuel type dialog: queue, remaining liters, BDT price ─────────
@Composable
private fun FuelInventoryDialog(
    station: FuelStationDto?,
    editing: FuelInventoryDto?,
    saving: Boolean,
    onSave: (fuelType: String, queue: Int, liters: Double, price: Double) -> Unit,
    onDismiss: () -> Unit
) {
    val existingTypes = station?.inventories?.map { it.fuelType }?.toSet() ?: emptySet()
    val allTypes = listOf("LPG", "DIESEL", "OCTANE", "PETROL")
    // new: pick a type not offered yet; edit: type is fixed
    val selectable = if (editing == null) allTypes.filter { it !in existingTypes } else listOf(editing.fuelType)

    var type by remember { mutableStateOf(selectable.firstOrNull() ?: "") }
    var queue by remember { mutableStateOf((editing?.queueCount ?: 0).toString()) }
    var litersText by remember {
        mutableStateOf(editing?.remainingLiters?.toBigDecimal()?.stripTrailingZeros()?.toPlainString() ?: "0")
    }
    var price by remember {
        mutableStateOf(editing?.pricePerLiter?.toBigDecimal()?.stripTrailingZeros()?.toPlainString() ?: "")
    }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = EvColors.Surface,
        title = {
            Text(
                if (editing == null) "Add Fuel Type" else "Update ${fuelLabel(editing.fuelType)}",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                if (editing == null) {
                    if (selectable.isEmpty()) {
                        Text("This station already offers every fuel type.",
                            style = MaterialTheme.typography.bodySmall, color = EvColors.OnSurfaceVar)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        selectable.forEach { t ->
                            FuelTypePill(fuelLabel(t), type == t) { type = t }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                }
                EvTextField(queue, { queue = it }, "Vehicles in queue *")
                Spacer(Modifier.height(8.dp))
                EvTextField(litersText, { litersText = it }, "Remaining (liters) *")
                Spacer(Modifier.height(8.dp))
                EvTextField(price, { price = it }, "Price (BDT per liter) *")
                error?.let {
                    Spacer(Modifier.height(8.dp))
                    Text(it, color = EvColors.Error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            EvPrimaryButton(
                text = if (saving) "Saving…" else "Save",
                onClick = {
                    val q = queue.toIntOrNull()
                    val l = litersText.toDoubleOrNull()
                    val p = price.toDoubleOrNull()
                    when {
                        type.isBlank() -> error = "Pick a fuel type"
                        q == null || q < 0 -> error = "Queue must be 0 or more"
                        l == null || l < 0 -> error = "Liters must be 0 or more"
                        p == null || p < 0 -> error = "Price must be 0 or more"
                        else -> {
                            error = null
                            onSave(type, q, l, p)
                        }
                    }
                },
                enabled = !saving && (editing != null || type.isNotBlank())
            )
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = EvColors.OnSurfaceVar) } }
    )
}

@Composable
private fun FuelTypePill(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        Modifier
            .clip(RoundedCornerShape(50))
            .background(if (selected) EvColors.PrimaryDim else EvColors.SurfaceHigh)
            .border(1.dp, if (selected) EvColors.Primary else EvColors.SurfaceBorder, RoundedCornerShape(50))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Text(label, color = if (selected) EvColors.Primary else EvColors.OnSurface,
            style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
private fun FuelInfoTag(text: String) {
    Row(
        Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(EvColors.SurfaceHigh)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(text, style = MaterialTheme.typography.labelSmall, color = EvColors.OnSurface)
    }
}
