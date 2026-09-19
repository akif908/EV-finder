package com.example.evfinder.feature.fuel

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.evfinder.core.model.FuelInventoryDto
import com.example.evfinder.core.model.FuelStationDto
import com.example.evfinder.ui.components.StatusPill
import com.example.evfinder.ui.theme.EvColors
import java.util.Locale

/**
 * Fuel Station card — queue, remaining liters and BDT price per fuel type.
 * Deliberately has NO booking button: fuel stations are not bookable.
 */
@Composable
fun FuelStationCard(station: FuelStationDto, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(EvColors.Surface)
            .border(1.dp, EvColors.SurfaceBorder, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(38.dp).clip(RoundedCornerShape(12.dp)).background(EvColors.PrimaryDim),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.LocalGasStation, null, tint = EvColors.Primary, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(station.name, style = MaterialTheme.typography.titleMedium,
                    color = EvColors.OnBackground, fontWeight = FontWeight.SemiBold)
                station.address?.let {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.LocationOn, null, tint = EvColors.OnSurfaceVar, modifier = Modifier.size(12.dp))
                        Spacer(Modifier.width(3.dp))
                        Text(it, style = MaterialTheme.typography.bodySmall, color = EvColors.OnSurfaceVar)
                    }
                }
            }
            StatusPill(label = if (station.isOpen) "Open" else "Closed", isActive = station.isOpen)
        }
        Spacer(Modifier.height(10.dp))
        if (station.inventories.isEmpty()) {
            Text("No fuel types listed yet.", style = MaterialTheme.typography.bodySmall, color = EvColors.OnSurfaceVar)
        }
        station.inventories.forEachIndexed { index, inv ->
            if (index > 0) {
                Spacer(Modifier.height(6.dp))
                androidx.compose.material3.HorizontalDivider(color = EvColors.SurfaceBorder.copy(0.5f))
                Spacer(Modifier.height(6.dp))
            }
            FuelInventoryBlock(inv)
        }
    }
}

/** One fuel type row: name, queue count, remaining liters (or Out of Stock), ৳/L. */
@Composable
fun FuelInventoryBlock(inv: FuelInventoryDto, modifier: Modifier = Modifier) {
    val outOfStock = inv.remainingLiters <= 0.0
    Row(
        modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(fuelLabel(inv.fuelType), style = MaterialTheme.typography.titleSmall,
                color = EvColors.OnBackground, fontWeight = FontWeight.SemiBold)
            Text(
                "Queue: ${inv.queueCount} vehicle" + if (inv.queueCount == 1) "" else "s",
                style = MaterialTheme.typography.bodySmall, color = EvColors.OnSurfaceVar
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                if (outOfStock) "Out of Stock" else "${liters(inv.remainingLiters)} L",
                style = MaterialTheme.typography.labelMedium,
                color = if (outOfStock) EvColors.Error else EvColors.OnSurface,
                fontWeight = FontWeight.SemiBold
            )
            Text("৳${bdt(inv.pricePerLiter)}/L", style = MaterialTheme.typography.bodySmall,
                color = EvColors.Primary, fontWeight = FontWeight.SemiBold)
        }
    }
}

fun fuelLabel(type: String) = when (type) {
    "LPG" -> "LPG"
    "DIESEL" -> "Diesel"
    "OCTANE" -> "Octane"
    "PETROL" -> "Petrol"
    else -> type
}

/** Liters with thousands grouping (1,250 L) — Bangladesh uses liters. */
fun liters(amount: Double): String = String.format(Locale.US, "%,.0f", amount)

/** BDT amount without trailing zeros (70, 121.5). */
fun bdt(amount: Double): String =
    amount.toBigDecimal().stripTrailingZeros().toPlainString()
