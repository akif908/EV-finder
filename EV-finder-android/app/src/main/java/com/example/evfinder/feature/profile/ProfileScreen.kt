package com.example.evfinder.feature.profile

import androidx.compose.foundation.background
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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Article
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ElectricCar
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.evfinder.ui.components.EvCard
import com.example.evfinder.ui.components.EvOutlinedButton
import com.example.evfinder.ui.components.EvTopBar
import com.example.evfinder.ui.components.StatusPill
import com.example.evfinder.ui.theme.EvColors

/**
 * Profile tab: account identity (from the stored JWT session), quick stats,
 * and logout. Real authorization always stays with the backend.
 */
@Composable
fun ProfileScreen(
    onLogout: () -> Unit,
    onOpenAdminNews: () -> Unit = {},
    viewModel: ProfileViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        EvTopBar(title = "Profile", subtitle = "Account & settings")

        // ---- identity card ----
        EvCard(Modifier.padding(horizontal = 16.dp)) {
            Row(
                Modifier.fillMaxWidth().padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(EvColors.PrimaryDim),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        state.name.take(1).uppercase(),
                        style = MaterialTheme.typography.titleLarge,
                        color = EvColors.Primary,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(Modifier.size(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(state.name, style = MaterialTheme.typography.titleMedium,
                        color = EvColors.OnBackground, fontWeight = FontWeight.SemiBold)
                    Text(state.email, style = MaterialTheme.typography.bodySmall,
                        color = EvColors.OnSurfaceVar)
                }
                StatusPill(
                    label = state.role.lowercase().replaceFirstChar { it.uppercase() },
                    isActive = state.role == "USER"
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        // ---- stats ----
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatCard("Bookings", state.bookingCount.toString(), Modifier.weight(1f))
            StatCard("Vehicles", state.vehicleCount.toString(), Modifier.weight(1f))
        }

        Spacer(Modifier.height(12.dp))

        state.error?.let {
            Text(it, color = EvColors.Error, style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(horizontal = 16.dp))
            Spacer(Modifier.height(8.dp))
        }

        // ---- settings rows (placeholders where the backend feature lands later) ----
        EvCard(Modifier.padding(horizontal = 16.dp)) {
            Column {
                if (state.role == "ADMIN") {
                    MenuRow(
                        Icons.Filled.Article, "News Management",
                        "Publish energy & fuel news",
                        onClick = onOpenAdminNews
                    )
                    HorizontalDivider(color = EvColors.SurfaceBorder)
                }
                MenuRow(Icons.Filled.CalendarMonth, "My Bookings", "View booking history")
                HorizontalDivider(color = EvColors.SurfaceBorder)
                MenuRow(Icons.Filled.ElectricCar, "Vehicles", "Manage your EVs")
                HorizontalDivider(color = EvColors.SurfaceBorder)
                MenuRow(Icons.Filled.Info, "App version", "1.0.0 — AOOP project")
            }
        }

        Spacer(Modifier.height(20.dp))

        EvOutlinedButton(
            text = "Log out",
            onClick = { viewModel.logout(onLogout) },
            modifier = Modifier.padding(horizontal = 16.dp).fillMaxWidth(),
            leadingContent = {
                Icon(Icons.AutoMirrored.Filled.Logout, null,
                    tint = EvColors.Error, modifier = Modifier.size(18.dp))
            }
        )

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun StatCard(label: String, value: String, modifier: Modifier = Modifier) {
    EvCard(modifier) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Text(value, style = MaterialTheme.typography.headlineSmall,
                color = EvColors.Primary, fontWeight = FontWeight.Bold)
            Text(label, style = MaterialTheme.typography.labelMedium,
                color = EvColors.OnSurfaceVar)
        }
    }
}

@Composable
private fun MenuRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: (() -> Unit)? = null
) {
    Row(
        Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = EvColors.OnSurfaceVar, modifier = Modifier.size(22.dp))
        Spacer(Modifier.size(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = EvColors.OnBackground)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = EvColors.OnSurfaceVar)
        }
    }
}
