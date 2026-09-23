package com.example.evfinder.feature.operator

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.evfinder.core.model.ServiceDto
import com.example.evfinder.core.model.StationDto
import com.example.evfinder.ui.components.EvTextField
import com.example.evfinder.ui.components.EvCard
import com.example.evfinder.ui.components.EvOutlinedButton
import com.example.evfinder.ui.components.EvPrimaryButton
import com.example.evfinder.ui.components.EvTopBar
import com.example.evfinder.ui.components.StatusPill
import com.example.evfinder.ui.theme.EvColors
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker

@Composable
fun OperatorStationsScreen(onSessionExpired: () -> Unit) {
    val vm: OperatorStationsViewModel = viewModel()
    val state by vm.uiState.collectAsState()

    Box(Modifier.fillMaxSize().background(EvColors.Background)) {
        Column(Modifier.fillMaxSize()) {
            EvTopBar(title = "My Stations", subtitle = "Create and manage your stations")
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
                    Text(state.error!!, color = EvColors.Error)
                    if (state.error!!.startsWith("Session expired")) EvPrimaryButton("Log in again", onSessionExpired)
                    else TextButton(onClick = vm::load) { Text("Retry", color = EvColors.Primary) }
                }
                else -> LazyColumn(
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 88.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Text(
                            "${state.myStationIds.size} of ${state.stations.size} stations are yours — manage those, view the rest",
                            style = MaterialTheme.typography.labelSmall, color = EvColors.OnSurfaceVar
                        )
                    }
                    items(state.stations, key = { it.id }) { station ->
                        if (station.id in state.myStationIds) {
                            OperatorStationCard(
                                station = station,
                                onEdit = { vm.openEdit(station) },
                                onStatus = { vm.openStatusDialog(station.id) },
                                onAddService = { vm.openServiceDialog(station.id, null, true) },
                                onEditService = { vm.openServiceDialog(station.id, it, false) },
                                onDeleteService = { vm.deleteService(station.id, it.id) }
                            )
                        } else OperatorStationReadOnlyCard(station)
                    }
                }
            }
        }
        EvPrimaryButton(
            text = "Add Station",
            icon = Icons.Filled.Add,
            onClick = vm::openCreate,
            modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp).fillMaxWidth()
        )
    }

    if (state.pickingLocation) {
        LocationPickerOverlay(onConfirm = vm::confirmLocation, onCancel = vm::cancelLocationPicker)
    }
    if (state.stationForm != null || state.stationFormIsNew) {
        StationFormDialog(
            editing = state.stationForm,
            pendingLat = state.pendingLat,
            pendingLng = state.pendingLng,
            saving = state.saving,
            onSave = vm::saveStation,
            onDismiss = vm::closeForms
        )
    }
    state.serviceDialogStationId?.let { stationId ->
        ServiceFormDialog(
            editing = state.serviceForm,
            isNew = state.serviceFormIsNew,
            saving = state.saving,
            onSave = { type, conn, kw, price, slots -> vm.saveService(stationId, type, conn, kw, price, slots) },
            onDismiss = vm::closeForms
        )
    }
    state.statusDialogStationId?.let { stationId ->
        StatusDialog(
            current = state.stations.firstOrNull { it.id == stationId }?.status ?: "ACTIVE",
            onSelect = { vm.changeStatus(stationId, it) },
            onDismiss = vm::closeStatusDialog
        )
    }
}

@Composable
private fun OperatorStationCard(
    station: StationDto,
    onEdit: () -> Unit,
    onStatus: () -> Unit,
    onAddService: () -> Unit,
    onEditService: (ServiceDto) -> Unit,
    onDeleteService: (ServiceDto) -> Unit
) {
    EvCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(station.name, style = MaterialTheme.typography.titleMedium, color = EvColors.OnBackground, fontWeight = FontWeight.SemiBold)
                    station.address?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = EvColors.OnSurfaceVar) }
                }
                StatusPill(label = if (station.status == "ACTIVE") "Open" else station.status.lowercase().replace('_', ' '), isActive = station.status == "ACTIVE")
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ActionChip("Edit", Icons.Filled.Edit, onEdit)
                ActionChip("Status", Icons.Filled.SwapHoriz, onStatus)
            }
            Spacer(Modifier.height(12.dp))
            HorizontalDivider(color = EvColors.SurfaceBorder)
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Services", style = MaterialTheme.typography.labelLarge, color = EvColors.OnSurfaceVar, modifier = Modifier.weight(1f))
                Text("+ add", color = EvColors.Primary, fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable(onClick = onAddService).padding(4.dp))
            }
            station.services.forEach { service ->
                Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Bolt, null, tint = EvColors.Primary, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Column(Modifier.weight(1f)) {
                        Text((if (service.serviceType == "BATTERY_SWAP") "Battery Swap" else "Charging") + (service.powerKw?.let { " · ${it.toInt()} kW" } ?: ""),
                            style = MaterialTheme.typography.bodyMedium, color = EvColors.OnSurface)
                        Text("৳${service.pricePerUnit.toBigDecimal().stripTrailingZeros().toPlainString()} · ${service.availableSlots} slots" + (service.connectorType?.let { " · $it" } ?: ""),
                            style = MaterialTheme.typography.labelSmall, color = EvColors.OnSurfaceVar)
                    }
                    IconButton(onClick = { onEditService(service) }, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Filled.Edit, "Edit", tint = EvColors.OnSurfaceVar, modifier = Modifier.size(16.dp))
                    }
                    IconButton(onClick = { onDeleteService(service) }, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Filled.Delete, "Delete", tint = EvColors.Error, modifier = Modifier.size(16.dp))
                    }
                }
            }
            if (station.services.isEmpty()) {
                Text("No services yet — add one so users can book.", style = MaterialTheme.typography.bodySmall, color = EvColors.OnSurfaceVar)
            }
        }
    }
}

@Composable
private fun OperatorStationReadOnlyCard(station: StationDto) {
    EvCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(station.name, style = MaterialTheme.typography.titleMedium, color = EvColors.OnBackground, fontWeight = FontWeight.SemiBold)
                    station.address?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = EvColors.OnSurfaceVar) }
                }
                StatusPill(label = "Other operator", isActive = false)
            }
        }
    }
}

@Composable
private fun ActionChip(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Row(
        Modifier.clip(RoundedCornerShape(10.dp)).background(EvColors.SurfaceHigh).clickable(onClick = onClick).padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(icon, null, tint = EvColors.OnSurfaceVar, modifier = Modifier.size(14.dp))
        Text(label, style = MaterialTheme.typography.labelMedium, color = EvColors.OnSurface)
    }
}

@Composable
// Shared with the fuel station module (feature.fuel).
fun LocationPickerOverlay(onConfirm: (Double, Double) -> Unit, onCancel: () -> Unit) {
    var picked by remember { mutableStateOf<GeoPoint?>(null) }
    val markerRef = remember { arrayOfNulls<Marker>(1) }
    Box(Modifier.fillMaxSize().background(EvColors.Background)) {
        AndroidView(
            factory = { ctx ->
                MapView(ctx).apply {
                    setTileSource(TileSourceFactory.MAPNIK)
                    setMultiTouchControls(true)
                    isTilesScaledToDpi = true
                    controller.setZoom(13.0)
                    controller.setCenter(GeoPoint(23.7810, 90.4150))
                    overlays.add(MapEventsOverlay(object : MapEventsReceiver {
                        override fun singleTapConfirmedHelper(p: GeoPoint?): Boolean {
                            p?.let { picked = it }
                            return true
                        }
                        override fun longPressHelper(p: GeoPoint?): Boolean = false
                    }))
                }
            },
            update = { map ->
                val p = picked
                if (p != null) {
                    val existing = markerRef[0]
                    if (existing == null) {
                        markerRef[0] = Marker(map).apply {
                            position = p
                            title = "Station location"
                            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                            map.overlays.add(this)
                        }
                    } else existing.position = p
                    map.invalidate()
                }
            },
            modifier = Modifier.matchParentSize()
        )
        Row(
            Modifier.align(Alignment.TopCenter).padding(top = 16.dp).clip(RoundedCornerShape(50))
                .background(EvColors.Background.copy(0.85f)).padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Filled.LocationOn, null, tint = EvColors.Primary, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(6.dp))
            Text(
                if (picked == null) "Tap the map to place your station" else "%.5f, %.5f".format(picked!!.latitude, picked!!.longitude),
                style = MaterialTheme.typography.labelSmall, color = EvColors.OnSurface
            )
        }
        Row(Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            EvOutlinedButton("Cancel", onCancel, Modifier.weight(1f))
            EvPrimaryButton(
                text = if (picked == null) "Pick a location" else "Confirm location",
                onClick = { picked?.let { onConfirm(it.latitude, it.longitude) } },
                enabled = picked != null,
                modifier = Modifier.weight(1.4f)
            )
        }
    }
}

@Composable
private fun StationFormDialog(
    editing: StationDto?,
    pendingLat: Double?,
    pendingLng: Double?,
    saving: Boolean,
    onSave: (name: String, desc: String, address: String, open: String, close: String) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(editing?.name ?: "") }
    var desc by remember { mutableStateOf(editing?.description ?: "") }
    var address by remember { mutableStateOf(editing?.address ?: "") }
    var open by remember { mutableStateOf(editing?.openingTime?.take(5) ?: "08:00") }
    var close by remember { mutableStateOf(editing?.closingTime?.take(5) ?: "22:00") }
    var error by remember { mutableStateOf<String?>(null) }
    val latText = pendingLat?.let { "%.5f".format(it) } ?: editing?.latitude?.let { "%.5f".format(it) }
    val lngText = pendingLng?.let { "%.5f".format(it) } ?: editing?.longitude?.let { "%.5f".format(it) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = EvColors.Surface,
        title = { Text(if (editing == null) "New Station" else "Edit Station", fontWeight = FontWeight.Bold) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    InfoTag("📍 lat: $latText")
                    InfoTag("📍 lng: $lngText")
                }
                Spacer(Modifier.height(12.dp))
                EvTextField(name, { name = it }, "Station name *")
                Spacer(Modifier.height(8.dp))
                EvTextField(address, { address = it }, "Address")
                Spacer(Modifier.height(8.dp))
                EvTextField(desc, { desc = it }, "Description")
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(Modifier.weight(1f)) { EvTextField(open, { open = it }, "Opens (HH:mm)") }
                    Box(Modifier.weight(1f)) { EvTextField(close, { close = it }, "Closes (HH:mm)") }
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
                    else { error = null; onSave(name, desc, address, open, close) }
                },
                enabled = !saving
            )
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = EvColors.OnSurfaceVar) } }
    )
}

@Composable
private fun ServiceFormDialog(
    editing: ServiceDto?,
    isNew: Boolean,
    saving: Boolean,
    onSave: (type: String, connector: String, powerKw: String, price: String, slots: String) -> Unit,
    onDismiss: () -> Unit
) {
    var type by remember { mutableStateOf(editing?.serviceType ?: "CHARGING") }
    var connector by remember { mutableStateOf(editing?.connectorType ?: "CCS2") }
    var powerKw by remember { mutableStateOf(editing?.powerKw?.toBigDecimal()?.stripTrailingZeros()?.toPlainString() ?: "60") }
    var price by remember { mutableStateOf(editing?.pricePerUnit?.toBigDecimal()?.stripTrailingZeros()?.toPlainString() ?: "") }
    var slots by remember { mutableStateOf(editing?.availableSlots?.toString() ?: "2") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = EvColors.Surface,
        title = { Text(if (isNew) "Add Service" else "Edit Service", fontWeight = FontWeight.Bold) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterPill("⚡ Charging", type == "CHARGING") { type = "CHARGING" }
                    FilterPill("🔄 Swap", type == "BATTERY_SWAP") { type = "BATTERY_SWAP" }
                }
                Spacer(Modifier.height(12.dp))
                if (type == "CHARGING") {
                    EvTextField(powerKw, { powerKw = it }, "Power (kW)")
                    Spacer(Modifier.height(8.dp))
                }
                EvTextField(connector, { connector = it }, "Connector (CCS2 / Type2)")
                Spacer(Modifier.height(8.dp))
                EvTextField(price, { price = it }, "Price per unit (৳) *")
                Spacer(Modifier.height(8.dp))
                EvTextField(slots, { slots = it }, "Concurrent slots *")
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
                    if (price.isBlank() || slots.isBlank()) error = "Price and slots are required"
                    else { error = null; onSave(type, connector, powerKw, price, slots) }
                },
                enabled = !saving
            )
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = EvColors.OnSurfaceVar) } }
    )
}

@Composable
private fun StatusDialog(current: String, onSelect: (String) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = EvColors.Surface,
        title = { Text("Station status", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text("Users only see ACTIVE stations.", style = MaterialTheme.typography.bodySmall, color = EvColors.OnSurfaceVar)
                Spacer(Modifier.height(12.dp))
                listOf(
                    "ACTIVE" to "Active — visible & bookable",
                    "TEMPORARILY_UNAVAILABLE" to "Temporarily unavailable",
                    "INACTIVE" to "Inactive — hidden from users"
                ).forEach { (value, label) ->
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
                            .background(if (current == value) EvColors.PrimaryDim else EvColors.SurfaceHigh)
                            .clickable { onSelect(value) }.padding(14.dp)
                    ) { Text(label, color = if (current == value) EvColors.Primary else EvColors.OnSurface) }
                    Spacer(Modifier.height(8.dp))
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close", color = EvColors.OnSurfaceVar) } }
    )
}

@Composable
private fun FilterPill(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        Modifier.clip(RoundedCornerShape(50))
            .background(if (selected) EvColors.PrimaryDim else EvColors.SurfaceHigh)
            .border(1.dp, if (selected) EvColors.Primary else EvColors.SurfaceBorder, RoundedCornerShape(50))
            .clickable(onClick = onClick).padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Text(label, color = if (selected) EvColors.Primary else EvColors.OnSurface, style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
private fun InfoTag(text: String) {
    Row(Modifier.clip(RoundedCornerShape(8.dp)).background(EvColors.SurfaceHigh).padding(horizontal = 8.dp, vertical = 4.dp)) {
        Text(text, style = MaterialTheme.typography.labelSmall, color = EvColors.OnSurface)
    }
}
