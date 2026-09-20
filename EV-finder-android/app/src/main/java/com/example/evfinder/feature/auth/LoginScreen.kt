package com.example.evfinder.feature.auth

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.evfinder.ui.components.*
import com.example.evfinder.ui.theme.EvColors

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

    Box(
        Modifier
            .fillMaxSize()
            .background(EvColors.Background)
    ) {
        // Subtle green radial glow at top
        Box(
            Modifier
                .size(320.dp)
                .offset(x = (-40).dp, y = (-60).dp)
                .background(
                    brush = Brush.radialGradient(
                        listOf(EvColors.Primary.copy(alpha = 0.12f), Color.Transparent)
                    )
                )
        )

        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 40.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // ── Top content ──────────────────────────────────────────────────
            Column {
                // Logo icon
                Box(
                    Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(EvColors.PrimaryDim)
                        .border(1.dp, EvColors.Primary.copy(0.3f), RoundedCornerShape(14.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Filled.Bolt,
                        contentDescription = null,
                        tint = EvColors.Primary,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Spacer(Modifier.height(28.dp))

                Text(
                    "Welcome back",
                    style = MaterialTheme.typography.headlineLarge,
                    color = EvColors.OnBackground,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "Sign in to access your garage, live station\nreservations & high-speed charging routes.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = EvColors.OnSurfaceVar,
                    lineHeight = 20.sp
                )

                Spacer(Modifier.height(32.dp))

                // ── Social buttons ──────────────────────────────────────────
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    EvOutlinedButton(
                        "Apple ID",
                        onClick = {},
                        modifier = Modifier.weight(1f),
                        leadingContent = {
                            Icon(Icons.Filled.PhoneAndroid, null, tint = EvColors.OnSurface, modifier = Modifier.size(18.dp))
                        }
                    )
                    EvOutlinedButton(
                        "Google",
                        onClick = {},
                        modifier = Modifier.weight(1f),
                        leadingContent = {
                            Icon(Icons.Filled.Public, null, tint = Color(0xFF4285F4), modifier = Modifier.size(18.dp))
                        }
                    )
                }

                Spacer(Modifier.height(24.dp))
                LabeledDivider("OR CONTINUE WITH EMAIL")
                Spacer(Modifier.height(24.dp))

                // ── Email field ─────────────────────────────────────────────
                Text("Email address", style = MaterialTheme.typography.labelMedium, color = EvColors.OnSurfaceVar)
                Spacer(Modifier.height(6.dp))
                EvTextField(
                    value = email,
                    onValueChange = { email = it },
                    placeholder = "you@example.com",
                    leadingIcon = Icons.Outlined.Email,
                    keyboardType = KeyboardType.Email,
                    trailingIcon = if (email.isNotBlank()) Icons.Filled.CheckCircle else null,
                    trailingTint = if (email.contains("@")) EvColors.Primary else EvColors.OnSurfaceVar
                )

                Spacer(Modifier.height(16.dp))

                // ── Password field ──────────────────────────────────────────
                Text("Password", style = MaterialTheme.typography.labelMedium, color = EvColors.OnSurfaceVar)
                Spacer(Modifier.height(6.dp))
                EvTextField(
                    value = password,
                    onValueChange = { password = it },
                    placeholder = "••••••••••••",
                    leadingIcon = Icons.Outlined.Lock,
                    keyboardType = KeyboardType.Password,
                    visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = if (showPassword) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                    onTrailingClick = { showPassword = !showPassword }
                )

                Spacer(Modifier.height(16.dp))

                // ── Remember + Forgot ───────────────────────────────────────
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = true, onCheckedChange = {},
                            colors = CheckboxDefaults.colors(checkedColor = EvColors.Primary)
                        )
                        Text("Remember device", style = MaterialTheme.typography.bodySmall, color = EvColors.OnSurface)
                    }
                    TextButton(onClick = {}) {
                        Text("Forgot password?", color = EvColors.Primary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                }

                Spacer(Modifier.height(8.dp))

                // ── Error ───────────────────────────────────────────────────
                AnimatedVisibility(visible = state.error != null) {
                    state.error?.let {
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(EvColors.Error.copy(0.1f))
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Filled.Warning, null, tint = EvColors.Error, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(it, color = EvColors.Error, style = MaterialTheme.typography.bodySmall)
                        }
                        Spacer(Modifier.height(12.dp))
                    }
                }

                Spacer(Modifier.height(8.dp))

                // ── CTA ─────────────────────────────────────────────────────
                EvPrimaryButton(
                    text = "Sign In to Garage  →",
                    onClick = { viewModel.login(email.trim(), password) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !state.loading && email.isNotBlank() && password.isNotBlank(),
                    loading = state.loading
                )

                Spacer(Modifier.height(20.dp))

                // ── Biometric ───────────────────────────────────────────────
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Or authenticate with biometrics", style = MaterialTheme.typography.bodySmall, color = EvColors.OnSurfaceVar)
                    Spacer(Modifier.height(12.dp))
                    Box(
                        Modifier
                            .size(52.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(EvColors.SurfaceHigh)
                            .border(1.dp, EvColors.SurfaceBorder, RoundedCornerShape(14.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.Fingerprint, null, tint = EvColors.Primary, modifier = Modifier.size(28.dp))
                    }
                }
            }

            // ── Bottom ───────────────────────────────────────────────────────
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Spacer(Modifier.height(24.dp))
                TextButton(onClick = onGoToRegister) {
                    Text(
                        buildAnnotatedString {
                            withStyle(SpanStyle(color = EvColors.OnSurfaceVar)) { append("Don't have an account? ") }
                            withStyle(SpanStyle(color = EvColors.Primary, fontWeight = FontWeight.Bold)) { append("Sign Up") }
                        }
                    )
                }
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Filled.Shield, null, tint = EvColors.Primary, modifier = Modifier.size(14.dp))
                    Text("256-bit encrypted EV Cloud Sync", style = MaterialTheme.typography.labelSmall, color = EvColors.OnSurfaceVar)
                }
            }
        }
    }
}

// ─── Shared styled text field ─────────────────────────────────────────────────
@Composable
fun EvTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    leadingIcon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    trailingIcon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    trailingTint: Color = EvColors.OnSurfaceVar,
    onTrailingClick: (() -> Unit)? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    singleLine: Boolean = true
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text(placeholder, color = EvColors.OnSurfaceVar.copy(0.5f)) },
        modifier = modifier.fillMaxWidth(),
        singleLine = singleLine,
        shape = RoundedCornerShape(14.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor   = EvColors.Primary,
            unfocusedBorderColor = EvColors.SurfaceBorder,
            focusedContainerColor   = EvColors.SurfaceHigh,
            unfocusedContainerColor = EvColors.SurfaceHigh,
            cursorColor          = EvColors.Primary,
            focusedTextColor     = EvColors.OnBackground,
            unfocusedTextColor   = EvColors.OnSurface,
            focusedLeadingIconColor  = EvColors.Primary,
            unfocusedLeadingIconColor= EvColors.OnSurfaceVar
        ),
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        visualTransformation = visualTransformation,
        leadingIcon = leadingIcon?.let { { Icon(it, null, modifier = Modifier.size(20.dp)) } },
        trailingIcon = trailingIcon?.let {
            {
                if (onTrailingClick != null) {
                    IconButton(onClick = onTrailingClick) { Icon(it, null, tint = trailingTint, modifier = Modifier.size(20.dp)) }
                } else {
                    Icon(it, null, tint = trailingTint, modifier = Modifier.size(20.dp))
                }
            }
        }
    )
}
