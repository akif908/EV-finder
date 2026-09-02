package com.example.evfinder.feature.station

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.evfinder.core.model.StationDto

/**
 * Station details + its active services. Selecting a service starts the
 * booking flow (Phase 4) — currently a no-op callback.
 */
@Composable
fun StationDetailScreen(
    stationId: String,
    onBookService: (stationId: String, serviceId: String) -> Unit
) {
    var station by remember { mutableStateOf<StationDto?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(true) }

    LaunchedEffect(stationId) {
        StationRepository().getStation(stationId).fold(
            onSuccess = { station = it },
            onFailure = { error = it.message }
        )
        loading = false
    }

    when {
        loading -> Column(
            Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) { CircularProgressIndicator() }

        error != null -> Column(
            Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) { Text(error!!, color = MaterialTheme.colorScheme.error) }

        station != null -> StationDetailContent(station!!, onBookService)
    }
}

@Composable
private fun StationDetailContent(station: StationDto, onBookService: (String, String) -> Unit) {
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Column {
                Text(station.name, style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                station.address?.let {
                    Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (station.openingTime != null && station.closingTime != null) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Open ${station.openingTime?.take(5)} – ${station.closingTime?.take(5)}",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
        item {
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
            Spacer(Modifier.height(8.dp))
            Text("Available Services", style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold)
        }
        if (station.services.isEmpty()) {
            item {
                Text(
                    "No active services at this station.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        items(station.services, key = { it.id }) { service ->
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            if (service.serviceType == "BATTERY_SWAP") "Battery Swap"
                            else "Charging" + (service.powerKw?.let { " · ${it.toInt()} kW" } ?: ""),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.weight(1f)
                        )
                        AssistChip(onClick = {}, label = {
                            Text(if (service.availableSlots > 0) "${service.availableSlots} slots" else "Full")
                        })
                    }
                    service.connectorType?.let {
                        Spacer(Modifier.height(4.dp))
                        Text("Connector: $it", style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "৳${service.pricePerUnit.toBigDecimal().stripTrailingZeros().toPlainString()} per unit",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.weight(1f)
                        )
                        androidx.compose.material3.Button(
                            onClick = { onBookService(station.id, service.id) },
                            enabled = service.availableSlots > 0
                        ) { Text("Book") }
                    }
                }
            }
        }
    }
}
