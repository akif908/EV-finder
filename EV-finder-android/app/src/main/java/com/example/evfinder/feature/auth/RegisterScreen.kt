package com.example.evfinder.feature.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.evfinder.ui.components.EvPrimaryButton
import com.example.evfinder.ui.components.EvTextField
import com.example.evfinder.ui.theme.EvColors

/**
 * Registration — segmented Driver / Station Operator control (the role the
 * backend stores), uppercase field labels, terms checkbox, bold CTA.
 */
@Composable
fun RegisterScreen(
    onAuthenticated: () -> Unit,
    onBackToLogin: () -> Unit,
    viewModel: AuthViewModel = viewModel()
) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var role by remember { mutableStateOf("USER") }
    var acceptedTerms by remember { mutableStateOf(false) }
    var showPassword by remember { mutableStateOf(false) }
    var localError by remember { mutableStateOf<String?>(null) }
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(state.success) { if (state.success) onAuthenticated() }

    Box(Modifier.fillMaxSize().background(EvColors.Background)) {
        Box(
            Modifier
                .size(280.dp)
                .align(Alignment.TopStart)
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
            Spacer(Modifier.height(16.dp))

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
                Text("VOLTAGE MOBILITY", style = MaterialTheme.typography.labelLarge,
                    color = EvColors.OnBackground, fontWeight = FontWeight.Bold)
            }

            Spacer(Modifier.height(32.dp))
            Text("Create Account", style = MaterialTheme.typography.headlineMedium,
                color = EvColors.OnBackground, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            Text("Initialize your secure access portal.", style = MaterialTheme.typography.bodyMedium,
                color = EvColors.OnSurfaceVar)

            Spacer(Modifier.height(28.dp))

            // ── account type: segmented control ──
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(EvColors.Background)
                    .border(1.dp, EvColors.SurfaceHighest, RoundedCornerShape(10.dp))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                SegmentedOption("Driver", role == "USER", Modifier.weight(1f)) { role = "USER" }
                SegmentedOption("Station Operator", role == "OPERATOR", Modifier.weight(1f)) { role = "OPERATOR" }
            }

            Spacer(Modifier.height(24.dp))

            EvTextField(name, { name = it }, "Jane Doe", label = "Full Name",
                leadingIcon = Icons.Filled.Person)
            Spacer(Modifier.height(16.dp))
            EvTextField(email, { email = it }, "jane@example.com", label = "Email Address",
                leadingIcon = Icons.Filled.Mail, keyboardType = KeyboardType.Email)
            Spacer(Modifier.height(16.dp))
            EvTextField(password, { password = it }, "••••••••", label = "Password",
                leadingIcon = Icons.Filled.Lock, keyboardType = KeyboardType.Password,
                visualTransformation = if (showPassword) VisualTransformation.None
                else PasswordVisualTransformation())
            // strength meter (mockup: 4 segment bars + hint)
            Spacer(Modifier.height(8.dp))
            PasswordStrengthMeter(password)

            Spacer(Modifier.height(16.dp))
            EvTextField(confirm, { confirm = it }, "••••••••", label = "Confirm Password",
                leadingIcon = Icons.Filled.Lock, keyboardType = KeyboardType.Password,
                visualTransformation = if (showPassword) VisualTransformation.None
                else PasswordVisualTransformation())
            Spacer(Modifier.height(8.dp))
            Row(
                Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { showPassword = !showPassword }
                    .padding(6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    if (showPassword) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                    null, tint = EvColors.OnSurfaceVar, modifier = Modifier.size(16.dp)
                )
                Text(if (showPassword) "Hide passwords" else "Show passwords",
                    style = MaterialTheme.typography.labelSmall, color = EvColors.OnSurfaceVar)
            }

            Spacer(Modifier.height(12.dp))

            // ── terms checkbox ──
            Row(
                Modifier.clickable { acceptedTerms = !acceptedTerms },
                verticalAlignment = Alignment.Top
            ) {
                Box(
                    Modifier
                        .size(20.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(if (acceptedTerms) EvColors.Primary else EvColors.SurfaceHigh)
                        .border(1.dp, if (acceptedTerms) EvColors.Primary else EvColors.SurfaceHighest,
                            RoundedCornerShape(4.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    if (acceptedTerms) Icon(Icons.Filled.Check, null,
                        tint = EvColors.OnPrimary, modifier = Modifier.size(14.dp))
                }
                Spacer(Modifier.width(12.dp))
                Text(
                    "I agree to the Terms of Service and Privacy Policy.",
                    style = MaterialTheme.typography.bodySmall,
                    color = EvColors.OnSurfaceVar
                )
            }

            val error = localError ?: state.error
            error?.let {
                Spacer(Modifier.height(12.dp))
                Text(it, color = EvColors.Error, style = MaterialTheme.typography.bodySmall)
            }

            Spacer(Modifier.height(20.dp))
            EvPrimaryButton(
                text = if (state.loading) "Creating account…" else "INITIALIZE ACCOUNT",
                onClick = {
                    localError = when {
                        name.isBlank() -> "Full name is required"
                        password.length < 6 -> "Password must be at least 6 characters"
                        password != confirm -> "Passwords do not match"
                        !acceptedTerms -> "Please accept the terms to continue"
                        else -> null
                    }
                    if (localError == null) {
                        viewModel.register(name.trim(), email.trim(), password, role)
                    }
                },
                enabled = !state.loading,
                loading = state.loading,
                trailingIcon = Icons.AutoMirrored.Filled.ArrowForward,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(24.dp))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Already have an account? ", style = MaterialTheme.typography.bodySmall,
                    color = EvColors.OnSurfaceVar)
                Text(
                    "Sign in",
                    style = MaterialTheme.typography.labelLarge,
                    color = EvColors.Primary,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable(onClick = onBackToLogin)
                        .padding(4.dp)
                )
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SegmentedOption(
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier
            .clip(RoundedCornerShape(7.dp))
            .background(if (selected) EvColors.Surface else androidx.compose.ui.graphics.Color.Transparent)
            .border(1.dp, if (selected) EvColors.SurfaceHighest else androidx.compose.ui.graphics.Color.Transparent,
                RoundedCornerShape(7.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) EvColors.OnSurface else EvColors.OnSurfaceVar,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}


/**
 * 4-segment strength meter (sign_up_mobile): length, digit, symbol, mixed case.
 */
@Composable
private fun PasswordStrengthMeter(password: String) {
    var score = 0
    if (password.length >= 8) score++
    if (password.any { it.isDigit() }) score++
    if (password.any { !it.isLetterOrDigit() }) score++
    if (password.any { it.isUpperCase() } && password.any { it.isLowerCase() }) score++

    val label = when {
        password.isEmpty() -> ""
        score <= 1 -> "Weak password"
        score == 2 -> "Fair password"
        score == 3 -> "Strong password"
        else -> "Very strong password"
    }
    val color = when {
        score <= 1 -> EvColors.Error
        score == 2 -> EvColors.Warning
        else -> EvColors.Primary
    }

    Column(Modifier.fillMaxWidth()) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            repeat(4) { index ->
                Box(
                    Modifier
                        .weight(1f)
                        .height(5.dp)
                        .clip(RoundedCornerShape(50))
                        .background(if (index < score) color else EvColors.SurfaceHighest)
                )
            }
        }
        if (label.isNotEmpty()) {
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    if (score >= 3) Icons.Filled.Check else Icons.Filled.Info,
                    null, tint = color, modifier = Modifier.size(13.dp)
                )
                Spacer(Modifier.width(5.dp))
                Text(label, style = MaterialTheme.typography.labelSmall, color = color)
                if (score < 4) {
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "Use 8+ chars with a number and symbol",
                        style = MaterialTheme.typography.labelSmall, color = EvColors.OnSurfaceVar
                    )
                }
            }
        }
    }
}
