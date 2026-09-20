package com.example.evfinder.feature.notifications

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.evfinder.core.model.NotificationDto
import com.example.evfinder.ui.components.EvCard
import com.example.evfinder.ui.components.EvIconTile
import com.example.evfinder.ui.components.LiveDot
import com.example.evfinder.ui.theme.EvColors

/**
 * Notifications (user + operator): icon-tile cards, unread accent border,
 * mark-all-read. Icons map per notification type.
 */
@Composable
fun NotificationsScreen(
    onBack: () -> Unit,
    viewModel: NotificationsViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    val state by viewModel.uiState.collectAsState()

    Column(Modifier.fillMaxSize().background(EvColors.Background)) {
        // header
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(11.dp))
                    .background(EvColors.Surface)
                    .clickable(onClick = onBack),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back",
                    tint = EvColors.OnSurface, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("Notifications", style = MaterialTheme.typography.titleLarge,
                    color = EvColors.OnBackground, fontWeight = FontWeight.Bold)
                Text(
                    if (state.unread > 0) "${state.unread} unread" else "All caught up",
                    style = MaterialTheme.typography.labelSmall, color = EvColors.OnSurfaceVar
                )
            }
            if (state.unread > 0) {
                Text(
                    "Mark all read",
                    color = EvColors.Primary,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(onClick = viewModel::markAllRead)
                        .padding(8.dp)
                )
            }
        }

        when {
            state.loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = EvColors.Primary)
            }

            state.error != null -> Column(
                Modifier.fillMaxSize().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(state.error!!, color = EvColors.Error)
                Spacer(Modifier.height(12.dp))
                TextButton(onClick = viewModel::load) { Text("Retry", color = EvColors.Primary) }
            }

            state.notifications.isEmpty() -> Box(
                Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(EvColors.Surface),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.Notifications, null,
                            tint = EvColors.OnSurfaceVar, modifier = Modifier.size(26.dp))
                    }
                    Spacer(Modifier.height(14.dp))
                    Text("No notifications yet", style = MaterialTheme.typography.titleMedium,
                        color = EvColors.OnBackground)
                    Text(
                        "Booking and review updates will appear here",
                        style = MaterialTheme.typography.bodySmall,
                        color = EvColors.OnSurfaceVar
                    )
                }
            }

            else -> LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(state.notifications, key = { it.id }) { n ->
                    NotificationCard(n, onClick = { if (!n.read) viewModel.markRead(n.id) })
                }
            }
        }
    }
}

@Composable
private fun NotificationCard(n: NotificationDto, onClick: () -> Unit) {
    // per-type icon tile + accent (mockup language)
    val (icon, accent) = when (n.type) {
        "BOOKING_CONFIRMED" -> "✅" to EvColors.Primary
        "BOOKING_CANCELLED" -> "🚫" to EvColors.Error
        "PAYMENT_FAILED" -> "💳" to EvColors.Warning
        "NEW_BOOKING" -> "📅" to EvColors.Primary
        "NEW_REVIEW" -> "⭐" to EvColors.Secondary
        else -> "🔔" to EvColors.OnSurfaceVar
    }

    EvCard(
        Modifier.fillMaxWidth().clickable(onClick = onClick),
        color = if (n.read) EvColors.Surface else EvColors.SurfaceHigh
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            EvIconTile(icon = Icons.Filled.Notifications, tint = accent, size = 42.dp,
                container = accent.copy(alpha = 0.12f))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        n.title,
                        style = MaterialTheme.typography.titleSmall,
                        color = if (n.read) EvColors.OnSurface else EvColors.OnBackground,
                        fontWeight = if (n.read) FontWeight.Medium else FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    if (!n.read) LiveDot(size = 7.dp)
                }
                Spacer(Modifier.height(2.dp))
                Text(
                    n.message,
                    style = MaterialTheme.typography.bodySmall,
                    color = EvColors.OnSurfaceVar
                )
                n.createdAt?.let {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        it.take(16).replace("T", " · "),
                        style = MaterialTheme.typography.labelSmall,
                        color = EvColors.OnSurfaceVar.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}
