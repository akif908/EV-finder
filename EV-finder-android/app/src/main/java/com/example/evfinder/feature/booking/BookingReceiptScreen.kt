package com.example.evfinder.feature.booking

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.EvStation
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.evfinder.core.model.BookingDto
import com.example.evfinder.feature.station.StationRepository
import com.example.evfinder.ui.components.EvCard
import com.example.evfinder.ui.components.EvIconTile
import com.example.evfinder.ui.components.EvPerforation
import com.example.evfinder.ui.components.EvStatusChip
import com.example.evfinder.ui.components.EvStepBar
import com.example.evfinder.ui.components.LiveDot
import com.example.evfinder.ui.components.SectionLabel
import com.example.evfinder.ui.theme.EvColors

/**
 * Booking receipt (booking_confirmed_mobile): celebration header, booking-ID
 * pill, ticket perforation, slot/output stat tiles, navigation card and the
 * secondary actions. Shown right after a successful (simulated) payment.
 */
@Composable
fun BookingReceiptScreen(
    bookingId: String,
    onBack: () -> Unit,
    onGetDirections: (lat: Double, lng: Double, name: String) -> Unit,
    onDone: () -> Unit
) {
    val repository = remember { BookingRepository() }
    var booking by remember { mutableStateOf<BookingDto?>(null) }
    var address by remember { mutableStateOf<String?>(null) }
    var coords by remember { mutableStateOf<Pair<Double, Double>?>(null) }
    var loading by remember { mutableStateOf(true) }

    LaunchedEffect(bookingId) {
        repository.booking(bookingId).fold(
            onSuccess = { b ->
                booking = b
                // station address + coordinates for the navigation card
                StationRepository().getStation(b.stationId).fold(
                    onSuccess = { st ->
                        address = st.address
                        coords = st.latitude to st.longitude
                    },
                    onFailure = { /* navigation card degrades gracefully */ }
                )
            },
            onFailure = { }
        )
        loading = false
    }

    val b = booking
    val reference = b?.id?.take(8)?.uppercase() ?: "—"

    Column(
        Modifier
            .fillMaxSize()
            .background(EvColors.Background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        // header
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(11.dp))
                    .background(EvColors.Surface)
                    .clickable(onClick = onBack),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = EvColors.OnSurface,
                    modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column {
                Text("Booking Receipt", style = MaterialTheme.typography.titleLarge,
                    color = EvColors.OnBackground, fontWeight = FontWeight.Bold)
                Text("Keep this for your session", style = MaterialTheme.typography.labelSmall,
                    color = EvColors.OnSurfaceVar)
            }
        }

        Spacer(Modifier.height(16.dp))

        // step complete strip
        EvStepBar(current = 6, total = 6,
            labels = listOf("Vehicle", "Station", "Service", "Time", "Payment", "Confirm"))

        Spacer(Modifier.height(16.dp))

        // ── hero celebration card ──
        EvCard(
            Modifier.fillMaxWidth(),
            color = EvColors.SurfaceHigh,
            cornerRadius = 12.dp
        ) {
            Column(
                Modifier.fillMaxWidth().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Box(
                        Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .background(EvColors.Primary.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(EvColors.Primary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.EvStation, null, tint = EvColors.OnPrimary,
                                modifier = Modifier.size(30.dp))
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                // booking reference pill
                Row(
                    Modifier
                        .clip(RoundedCornerShape(50))
                        .background(EvColors.SurfaceLowest)
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    LiveDot(size = 6.dp)
                    Spacer(Modifier.width(7.dp))
                    Text(
                        "#$reference",
                        style = MaterialTheme.typography.labelSmall,
                        color = EvColors.Primary,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.5.sp
                    )
                }

                Spacer(Modifier.height(12.dp))
                Text("Charge Secured", style = MaterialTheme.typography.titleLarge,
                    color = EvColors.OnBackground, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                Text(
                    b?.stationName ?: "Your station",
                    style = MaterialTheme.typography.labelLarge, color = EvColors.Secondary
                )

                Spacer(Modifier.height(18.dp))
                EvPerforation()

                Spacer(Modifier.height(14.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ReceiptStat("Slot window",
                        b?.let { "${it.startTime.substring(11, 16)} – ${it.endTime.substring(11, 16)}" } ?: "—",
                        Icons.Filled.Schedule, Modifier.weight(1f))
                    ReceiptStat("Service",
                        if (b?.serviceName == "BATTERY_SWAP") "Battery swap" else "Charging",
                        Icons.Filled.Bolt, Modifier.weight(1f), valueColor = EvColors.Primary)
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // ── station & navigation card ──
        EvCard(Modifier.fillMaxWidth(), color = EvColors.Surface) {
            Column(Modifier.fillMaxWidth()) {
                Row(
                    Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    EvIconTile(Icons.Filled.EvStation, size = 42.dp)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(b?.stationName ?: "—", style = MaterialTheme.typography.titleMedium,
                            color = EvColors.OnBackground, fontWeight = FontWeight.SemiBold)
                        Text(address ?: "Address unavailable",
                            style = MaterialTheme.typography.bodySmall, color = EvColors.OnSurfaceVar)
                    }
                }

                // detail chips: date, slot, reference
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    DetailChip("Date", b?.startTime?.take(10) ?: "—", Modifier.weight(1f))
                    DetailChip("From", b?.startTime?.substring(11, 16) ?: "—", Modifier.weight(1f))
                    DetailChip("Ref", reference, Modifier.weight(1f), valueColor = EvColors.Primary)
                }
                Spacer(Modifier.height(14.dp))

                // actions
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    com.example.evfinder.ui.components.EvWideButton(
                        text = "Get Directions",
                        icon = Icons.Filled.NearMe,
                        enabled = coords != null,
                        onClick = {
                            coords?.let { (lat, lng) ->
                                onGetDirections(lat, lng, b?.stationName ?: "Station")
                            }
                        },
                        modifier = Modifier.weight(1f)
                    )
                    com.example.evfinder.ui.components.EvWideButton(
                        text = "My Bookings",
                        container = EvColors.SurfaceHigh,
                        contentColor = EvColors.OnSurface,
                        onClick = onDone,
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(Modifier.height(16.dp))
            }
        }

        Spacer(Modifier.height(16.dp))

        // ── QR / pass hint ──
        EvCard(Modifier.fillMaxWidth(), color = EvColors.SurfaceLow) {
            Row(
                Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                EvIconTile(Icons.Filled.QrCode2, size = 40.dp)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("Show at the station", style = MaterialTheme.typography.titleSmall,
                        color = EvColors.OnSurface, fontWeight = FontWeight.SemiBold)
                    Text("Quote reference #$reference to the operator to start your session",
                        style = MaterialTheme.typography.labelSmall, color = EvColors.OnSurfaceVar)
                }
                EvStatusChip("Confirmed", icon = Icons.Filled.Check)
            }
        }

        Spacer(Modifier.height(16.dp))

        // ── pro tip ──
        EvCard(Modifier.fillMaxWidth(), color = EvColors.SurfaceLow) {
            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.Top) {
                EvIconTile(Icons.Filled.Info, tint = EvColors.Secondary, size = 34.dp)
                Spacer(Modifier.width(12.dp))
                Column {
                    Text("Pro tip", style = MaterialTheme.typography.labelLarge,
                        color = EvColors.OnSurface, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(2.dp))
                    Text(
                        "Arrive a few minutes early — your slot is held briefly past the start time before being released.",
                        style = MaterialTheme.typography.bodySmall, color = EvColors.OnSurfaceVar
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // payment line
        if (loading) {
            Text("Loading receipt…", style = MaterialTheme.typography.labelSmall,
                color = EvColors.OnSurfaceVar)
        } else {
            SectionLabel("Paid ৳${(b?.amount ?: 0.0).toBigDecimal().stripTrailingZeros().toPlainString()} · simulated gateway")
        }

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun ReceiptStat(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier,
    valueColor: androidx.compose.ui.graphics.Color = EvColors.OnSurface
) {
    Column(
        modifier
            .clip(RoundedCornerShape(8.dp))
            .background(EvColors.SurfaceLowest)
            .padding(vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = EvColors.OnSurfaceVar, modifier = Modifier.size(12.dp))
            Spacer(Modifier.width(4.dp))
            Text(label.uppercase(), style = MaterialTheme.typography.labelSmall,
                color = EvColors.OnSurfaceVar, fontSize = 10.sp)
        }
        Spacer(Modifier.height(3.dp))
        Text(value, style = MaterialTheme.typography.labelLarge, color = valueColor,
            fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun DetailChip(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    valueColor: androidx.compose.ui.graphics.Color = EvColors.OnSurface
) {
    Column(
        modifier
            .clip(RoundedCornerShape(8.dp))
            .background(EvColors.SurfaceLow)
            .padding(horizontal = 10.dp, vertical = 8.dp)
    ) {
        Text(label.uppercase(), style = MaterialTheme.typography.labelSmall,
            color = EvColors.OnSurfaceVar, fontSize = 10.sp)
        Spacer(Modifier.height(2.dp))
        Text(value, style = MaterialTheme.typography.labelLarge, color = valueColor,
            fontWeight = FontWeight.SemiBold, maxLines = 1)
    }
}
