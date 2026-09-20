package com.example.evfinder.feature.auth

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.evfinder.ui.components.EvPrimaryButton
import com.example.evfinder.ui.components.EvTextField
import com.example.evfinder.ui.components.LabeledDivider
import com.example.evfinder.ui.theme.EvColors

/**
 * Login — Voltage Mobility design: brand mark, headline, uppercase field
 * labels with leading icons, green CTA, footer sign-up link.
 */
@Composable
fun LoginScreen(
    onAuthenticated: () -> Unit,
    onGoToRegister: () -> Unit,
    viewModel: AuthViewModel = viewModel()
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(state.success) { if (state.success) onAuthenticated() }

    Box(Modifier.fillMaxSize().background(EvColors.Background)) {
        // ambient green glow
        Box(
            Modifier
                .size(320.dp)
                .align(Alignment.TopEnd)
                .padding(top = 40.dp, end = 20.dp)
                .clip(RoundedCornerShape(50))
                .background(EvColors.Primary.copy(alpha = 0.05f))
        )

        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 32.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(Modifier.height(24.dp))

            // ── brand mark ──
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(
                    Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(EvColors.Primary.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.Bolt, null, tint = EvColors.Primary, modifier = Modifier.size(26.dp))
                }
                Text(
                    "VOLTAGE MOBILITY",
                    style = MaterialTheme.typography.labelLarge,
                    color = EvColors.OnBackground,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(Modifier.height(40.dp))

            // ── headline ──
            Text(
                "Welcome Back",
                style = MaterialTheme.typography.headlineLarge,
                color = EvColors.OnBackground,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Log in to find charging stations and manage your bookings.",
                style = MaterialTheme.typography.bodyMedium,
                color = EvColors.OnSurfaceVar
            )

            Spacer(Modifier.height(32.dp))

            // ── form ──
            EvTextField(
                value = email,
                onValueChange = { email = it },
                placeholder = "name@company.com",
                label = "Work Email",
                leadingIcon = Icons.Filled.Mail,
                keyboardType = KeyboardType.Email
            )
            Spacer(Modifier.height(16.dp))
            EvTextField(
                value = password,
                onValueChange = { password = it },
                placeholder = "••••••••",
                label = "Password",
                leadingIcon = Icons.Filled.Lock,
                keyboardType = KeyboardType.Password,
                visualTransformation = if (showPassword) VisualTransformation.None
                else PasswordVisualTransformation()
            )
            // show/hide toggle row
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                Row(
                    Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { showPassword = !showPassword }
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        if (showPassword) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                        null, tint = EvColors.OnSurfaceVar, modifier = Modifier.size(16.dp)
                    )
                    Text(
                        if (showPassword) "Hide password" else "Show password",
                        style = MaterialTheme.typography.labelSmall,
                        color = EvColors.OnSurfaceVar
                    )
                }
            }

            state.error?.let {
                Spacer(Modifier.height(8.dp))
                Text(it, color = EvColors.Error, style = MaterialTheme.typography.bodySmall)
            }

            Spacer(Modifier.height(16.dp))
            EvPrimaryButton(
                text = if (state.loading) "Logging in…" else "Sign In",
                onClick = { viewModel.login(email.trim(), password) },
                enabled = !state.loading && email.isNotBlank() && password.isNotBlank(),
                loading = state.loading,
                trailingIcon = Icons.AutoMirrored.Filled.ArrowForward,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(24.dp))
            LabeledDivider("Or")

            Spacer(Modifier.height(20.dp))

            // demo credentials helper — handy for the viva
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(EvColors.Surface)
                    .padding(14.dp)
            ) {
                Text("Demo accounts", style = MaterialTheme.typography.labelSmall,
                    color = EvColors.OnSurfaceVar, letterSpacing = 0.8.sp)
                Spacer(Modifier.height(6.dp))
                listOf(
                    "User" to "test@ev.com / secret123",
                    "Operator" to "operator@ev.com / operator123",
                    "Admin" to "admin@ev.com / admin123"
                ).forEach { (role, creds) ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
                        Text(role, style = MaterialTheme.typography.labelMedium,
                            color = EvColors.Primary, modifier = Modifier.width(76.dp))
                        Text(creds, style = MaterialTheme.typography.bodySmall, color = EvColors.OnSurface)
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            // ── footer ──
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Don't have an account? ", style = MaterialTheme.typography.bodySmall,
                    color = EvColors.OnSurfaceVar)
                Text(
                    "Sign up",
                    style = MaterialTheme.typography.labelLarge,
                    color = EvColors.Primary,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable(onClick = onGoToRegister)
                        .padding(4.dp)
                )
            }
        }
    }
}
