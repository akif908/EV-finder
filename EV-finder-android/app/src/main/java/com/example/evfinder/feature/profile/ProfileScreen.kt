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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ElectricCar
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.evfinder.ui.components.EvTextField
import com.example.evfinder.ui.components.EvCard
import com.example.evfinder.ui.components.EvSectionHeader
import com.example.evfinder.ui.components.EvSettingRow
import com.example.evfinder.ui.components.EvToggle
import com.example.evfinder.ui.components.LiveDot
import com.example.evfinder.ui.components.EvOutlinedButton
import com.example.evfinder.ui.components.EvPrimaryButton
import com.example.evfinder.ui.components.EvTopBar
import com.example.evfinder.ui.components.StatusPill
import com.example.evfinder.ui.theme.EvColors
import kotlinx.coroutines.delay

/**
 * Profile (shared by all roles): identity card, editable info,
 * password change, stats and logout.
 */
@Composable
fun ProfileScreen(
    onLogout: () -> Unit,
    viewModel: ProfileViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()
    // notification preferences (device-local for now)
    var bookingAlerts by remember { mutableStateOf(true) }
    var reviewAlerts by remember { mutableStateOf(true) }

    // transient toast auto-clears
    LaunchedEffect(state.toast) {
        if (state.toast != null) { delay(2500); viewModel.closeDialogs() }
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        EvTopBar(title = "Profile", subtitle = "Account & settings")

        when {
            state.loading -> Box(Modifier.fillMaxSize().padding(48.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = EvColors.Primary)
            }

            else -> {
                val me = state.me

                // toast
                state.toast?.let {
                    Text(it, color = EvColors.Primary, style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.padding(horizontal = 16.dp))
                    Spacer(Modifier.height(8.dp))
                }

                // ---- identity card ----
                EvCard(Modifier.padding(horizontal = 16.dp)) {
                    Column(Modifier.fillMaxWidth().padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                Modifier
                                    .size(56.dp)
                                    .clip(CircleShape)
                                    .background(EvColors.PrimaryDim),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    (me?.name ?: "U").take(1).uppercase(),
                                    style = MaterialTheme.typography.titleLarge,
                                    color = EvColors.Primary, fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(Modifier.size(14.dp))
                            Column(Modifier.weight(1f)) {
                                Text(me?.name ?: "—", style = MaterialTheme.typography.titleMedium,
                                    color = EvColors.OnBackground, fontWeight = FontWeight.SemiBold)
                                Text(me?.email ?: "—", style = MaterialTheme.typography.bodySmall,
                                    color = EvColors.OnSurfaceVar)
                            }
                            StatusPill(
                                label = (me?.role ?: "USER").lowercase().replaceFirstChar { it.uppercase() },
                                isActive = me?.role == "USER"
                            )
                        }
                        me?.phone?.let {
                            Spacer(Modifier.height(10.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.Phone, null, tint = EvColors.OnSurfaceVar, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(it, style = MaterialTheme.typography.bodyMedium, color = EvColors.OnSurface)
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            EvOutlinedButton("Edit profile", viewModel::openEdit, Modifier.weight(1f),
                                leadingContent = { Icon(Icons.Filled.Edit, null, tint = EvColors.Primary, modifier = Modifier.size(16.dp)) })
                            EvOutlinedButton("Password", viewModel::openPassword, Modifier.weight(1f),
                                leadingContent = { Icon(Icons.Filled.Lock, null, tint = EvColors.OnSurfaceVar, modifier = Modifier.size(16.dp)) })
                        }
                    }
                }

                state.error?.let {
                    Spacer(Modifier.height(8.dp))
                    Text(it, color = EvColors.Error, style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(horizontal = 16.dp))
                }

                // ---- stats ----
                Spacer(Modifier.height(12.dp))
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatCard("Bookings", state.bookingCount.toString(), Modifier.weight(1f))
                    StatCard("Vehicles", state.vehicleCount.toString(), Modifier.weight(1f))
                }

                // ---- account section ----
                Spacer(Modifier.height(16.dp))
                EvSectionHeader("Account", Modifier.padding(horizontal = 16.dp),
                    icon = Icons.Filled.Person)
                Spacer(Modifier.height(8.dp))
                EvCard(Modifier.padding(horizontal = 16.dp)) {
                    Column {
                        EvSettingRow(
                            Icons.Filled.CalendarMonth, "Member since",
                            me?.createdAt?.take(10) ?: "—",
                            accent = EvColors.Secondary
                        )
                        HorizontalDivider(color = EvColors.SurfaceBorder)
                        EvSettingRow(
                            Icons.Filled.Badge, "Role",
                            me?.role?.lowercase()?.replaceFirstChar { it.uppercase() } ?: "USER",
                            accent = EvColors.Tertiary
                        )
                        HorizontalDivider(color = EvColors.SurfaceBorder)
                        EvSettingRow(
                            Icons.Filled.Lock, "Password",
                            "Change your account password",
                            onClick = viewModel::openPassword
                        )
                        HorizontalDivider(color = EvColors.SurfaceBorder)
                        EvSettingRow(
                            Icons.Filled.Edit, "Edit profile",
                            "Name and phone number",
                            onClick = viewModel::openEdit
                        )
                    }
                }

                // ---- notifications section ----
                Spacer(Modifier.height(16.dp))
                EvSectionHeader("Notifications", Modifier.padding(horizontal = 16.dp),
                    icon = Icons.Filled.Notifications)
                Spacer(Modifier.height(8.dp))
                EvCard(Modifier.padding(horizontal = 16.dp)) {
                    Column {
                        EvSettingRow(
                            Icons.Filled.Notifications, "Booking updates",
                            "Confirmations, cancellations and reminders",
                            trailing = { EvToggle(bookingAlerts) { bookingAlerts = it } }
                        )
                        HorizontalDivider(color = EvColors.SurfaceBorder)
                        EvSettingRow(
                            Icons.Filled.Star, "Review prompts",
                            "Remind me to rate a station after charging",
                            accent = EvColors.Secondary,
                            trailing = { EvToggle(reviewAlerts) { reviewAlerts = it } }
                        )
                    }
                }

                // ---- appearance section ----
                Spacer(Modifier.height(16.dp))
                EvSectionHeader("Appearance", Modifier.padding(horizontal = 16.dp),
                    icon = Icons.Filled.DarkMode)
                Spacer(Modifier.height(8.dp))
                EvCard(Modifier.padding(horizontal = 16.dp)) {
                    EvSettingRow(
                        Icons.Filled.DarkMode, "Theme",
                        "Optimised midnight palette",
                        accent = EvColors.Secondary,
                        trailing = {
                            Row(
                                Modifier
                                    .clip(RoundedCornerShape(50))
                                    .background(EvColors.SurfaceLow)
                                    .padding(horizontal = 10.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                LiveDot(size = 6.dp)
                                Spacer(Modifier.width(5.dp))
                                Text("Dark active", style = MaterialTheme.typography.labelSmall,
                                    color = EvColors.Primary)
                            }
                        }
                    )
                }

                Spacer(Modifier.height(16.dp))
                EvCard(Modifier.padding(horizontal = 16.dp), color = EvColors.SurfaceLow) {
                    EvSettingRow(
                        Icons.Filled.Info, "App version",
                        "1.0.0 — AOOP course project",
                        accent = EvColors.OnSurfaceVar
                    )
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
    }

    // ---- edit profile dialog ----
    if (state.showEdit) {
        var name by remember { mutableStateOf(state.me?.name ?: "") }
        var phone by remember { mutableStateOf(state.me?.phone ?: "") }
        var localError by remember { mutableStateOf<String?>(null) }
        AlertDialog(
            onDismissRequest = viewModel::closeDialogs,
            containerColor = EvColors.Surface,
            title = { Text("Edit profile", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    EvTextField(name, { name = it }, "Full name *")
                    Spacer(Modifier.height(8.dp))
                    EvTextField(phone, { phone = it }, "Phone (e.g. 01712345678)",
                        keyboardType = KeyboardType.Phone)
                    localError?.let {
                        Spacer(Modifier.height(8.dp))
                        Text(it, color = EvColors.Error, style = MaterialTheme.typography.bodySmall)
                    }
                    state.error?.let {
                        Spacer(Modifier.height(8.dp))
                        Text(it, color = EvColors.Error, style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            confirmButton = {
                EvPrimaryButton(
                    text = if (state.saving) "Saving…" else "Save",
                    onClick = {
                        if (name.isBlank()) localError = "Name is required"
                        else { localError = null; viewModel.saveProfile(name, phone) }
                    },
                    enabled = !state.saving
                )
            },
            dismissButton = { TextButton(onClick = viewModel::closeDialogs) { Text("Cancel", color = EvColors.OnSurfaceVar) } }
        )
    }

    // ---- change password dialog ----
    if (state.showPassword) {
        var oldPw by remember { mutableStateOf("") }
        var newPw by remember { mutableStateOf("") }
        var confirmPw by remember { mutableStateOf("") }
        var localError by remember { mutableStateOf<String?>(null) }
        AlertDialog(
            onDismissRequest = viewModel::closeDialogs,
            containerColor = EvColors.Surface,
            title = { Text("Change password", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    EvTextField(oldPw, { oldPw = it }, "Current password *",
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardType = KeyboardType.Password)
                    Spacer(Modifier.height(8.dp))
                    EvTextField(newPw, { newPw = it }, "New password (min 6) *",
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardType = KeyboardType.Password)
                    Spacer(Modifier.height(8.dp))
                    EvTextField(confirmPw, { confirmPw = it }, "Confirm new password *",
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardType = KeyboardType.Password)
                    localError?.let {
                        Spacer(Modifier.height(8.dp))
                        Text(it, color = EvColors.Error, style = MaterialTheme.typography.bodySmall)
                    }
                    state.error?.let {
                        Spacer(Modifier.height(8.dp))
                        Text(it, color = EvColors.Error, style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            confirmButton = {
                EvPrimaryButton(
                    text = if (state.saving) "Changing…" else "Change",
                    onClick = {
                        localError = when {
                            oldPw.isBlank() -> "Enter your current password"
                            newPw.length < 6 -> "New password must be at least 6 characters"
                            newPw != confirmPw -> "New passwords don't match"
                            else -> null
                        }
                        if (localError == null) viewModel.changePassword(oldPw, newPw)
                    },
                    enabled = !state.saving
                )
            },
            dismissButton = { TextButton(onClick = viewModel::closeDialogs) { Text("Cancel", color = EvColors.OnSurfaceVar) } }
        )
    }
}

@Composable
private fun StatCard(label: String, value: String, modifier: Modifier = Modifier) {
    EvCard(modifier) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Text(value, style = MaterialTheme.typography.headlineSmall,
                color = EvColors.Primary, fontWeight = FontWeight.Bold)
            Text(label, style = MaterialTheme.typography.labelMedium, color = EvColors.OnSurfaceVar)
        }
    }
}

@Composable
private fun MenuRow(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, subtitle: String) {
    Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = EvColors.OnSurfaceVar, modifier = Modifier.size(22.dp))
        Spacer(Modifier.size(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = EvColors.OnBackground)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = EvColors.OnSurfaceVar)
        }
    }
}
