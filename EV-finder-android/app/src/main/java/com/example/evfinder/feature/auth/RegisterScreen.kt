package com.example.evfinder.feature.auth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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

private val evModels = listOf(
    "Tesla Model 3 / Y (NACS)",
    "Hyundai Ioniq 5",
    "Rivian R1T",
    "Ford Mach-E",
    "Nissan Leaf",
    "Chevy Bolt"
)

@Composable
fun RegisterScreen(
    onAuthenticated: () -> Unit,
    onBackToLogin: () -> Unit,
    viewModel: AuthViewModel = viewModel()
) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var role by remember { mutableStateOf("USER") }
    var selectedModel by remember { mutableStateOf<String?>(null) }
    var agreedTerms by remember { mutableStateOf(false) }
    val state by viewModel.uiState.collectAsState()

    // Password strength
    val pwStrength = when {
        password.length >= 10 && password.any { it.isUpperCase() } && password.any { !it.isLetterOrDigit() } -> 3
        password.length >= 6  && (password.any { it.isDigit() } || password.any { it.isUpperCase() }) -> 2
        password.isNotEmpty() -> 1
        else -> 0
    }
    val pwLabel = listOf("", "Weak", "Good", "Strong")[pwStrength]
    val pwColors = listOf(Color.Transparent, EvColors.Error, EvColors.Warning, EvColors.Primary)

    LaunchedEffect(state.success) { if (state.success) onAuthenticated() }

    Box(
        Modifier
            .fillMaxSize()
            .background(EvColors.Background)
    ) {
        // Glow
        Box(
            Modifier
                .size(280.dp)
                .offset(x = 200.dp, y = (-30).dp)
                .background(Brush.radialGradient(listOf(EvColors.Primary.copy(0.09f), Color.Transparent)))
        )

        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 36.dp)
        ) {
            // ── Badge ──────────────────────────────────────────────────────
            Row(
                Modifier
                    .clip(RoundedCornerShape(50))
                    .background(EvColors.PrimaryDim)
                    .padding(horizontal = 10.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(Icons.Filled.Bolt, null, tint = EvColors.Primary, modifier = Modifier.size(12.dp))
                Text("V 2.4 LIVE GRID", style = MaterialTheme.typography.labelSmall, color = EvColors.Primary, letterSpacing = 0.8.sp)
                Spacer(Modifier.width(6.dp))
                Icon(Icons.Filled.Bolt, null, tint = EvColors.OnSurfaceVar, modifier = Modifier.size(12.dp))
                Text("28k+ Fast Plugs", style = MaterialTheme.typography.labelSmall, color = EvColors.OnSurfaceVar)
            }

            Spacer(Modifier.height(20.dp))

            Text(
                "Create your account",
                style = MaterialTheme.typography.headlineLarge,
                color = EvColors.OnBackground,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Join the next-generation EV charging and\nbattery-swap network across 42 countries.",
                style = MaterialTheme.typography.bodyMedium,
                color = EvColors.OnSurfaceVar,
                lineHeight = 20.sp
            )

            Spacer(Modifier.height(24.dp))

            // ── Social buttons ────────────────────────────────────────────
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

            Spacer(Modifier.height(20.dp))
            LabeledDivider("OR REGISTER WITH EMAIL")
            Spacer(Modifier.height(20.dp))

            // ── Name ────────────────────────────────────────────────────
            Text("Full Name", style = MaterialTheme.typography.labelMedium, color = EvColors.OnSurfaceVar)
            Spacer(Modifier.height(6.dp))
            EvTextField(
                value = name,
                onValueChange = { name = it },
                placeholder = "Julian Alexander",
                leadingIcon = Icons.Outlined.Person
            )
            Spacer(Modifier.height(14.dp))

            // ── Email ────────────────────────────────────────────────────
            Text("Email Address", style = MaterialTheme.typography.labelMedium, color = EvColors.OnSurfaceVar)
            Spacer(Modifier.height(6.dp))
            EvTextField(
                value = email,
                onValueChange = { email = it },
                placeholder = "julian.ev@mobility.io",
                leadingIcon = Icons.Outlined.Email,
                keyboardType = KeyboardType.Email,
                trailingIcon = if (email.contains("@")) Icons.Filled.CheckCircle else null,
                trailingTint = EvColors.Primary
            )
            Spacer(Modifier.height(14.dp))

            // ── Password ─────────────────────────────────────────────────
            Text("Password", style = MaterialTheme.typography.labelMedium, color = EvColors.OnSurfaceVar)
            Spacer(Modifier.height(6.dp))
            EvTextField(
                value = password,
                onValueChange = { password = it },
                placeholder = "Min 6 characters",
                leadingIcon = Icons.Outlined.Lock,
                keyboardType = KeyboardType.Password,
                visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = if (showPassword) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                onTrailingClick = { showPassword = !showPassword }
            )
            if (password.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    repeat(3) { idx ->
                        Box(
                            Modifier
                                .weight(1f)
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(if (idx < pwStrength) pwColors[pwStrength] else EvColors.SurfaceBorder)
                        )
                    }
                }
                Spacer(Modifier.height(4.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        if (pwStrength >= 2) Icon(Icons.Filled.CheckCircle, null, tint = pwColors[pwStrength], modifier = Modifier.size(12.dp))
                        Text(pwLabel, color = pwColors[pwStrength], style = MaterialTheme.typography.labelSmall)
                    }
                    if (pwStrength >= 2)
                        Text("Includes numbers & symbols", style = MaterialTheme.typography.labelSmall, color = EvColors.OnSurfaceVar)
                }
            }
            Spacer(Modifier.height(14.dp))

            // ── Role chips ────────────────────────────────────────────────
            Text("I am a:", style = MaterialTheme.typography.labelMedium, color = EvColors.OnSurfaceVar)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                listOf("USER" to "EV Owner", "OPERATOR" to "Station Operator").forEach { (value, label) ->
                    val selected = role == value
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(50))
                            .background(if (selected) EvColors.PrimaryDim else EvColors.SurfaceHigh)
                            .border(
                                1.dp,
                                if (selected) EvColors.Primary else EvColors.SurfaceBorder,
                                RoundedCornerShape(50)
                            )
                            .clickable { role = value }
                            .padding(horizontal = 16.dp, vertical = 10.dp)
                    ) {
                        Text(
                            label,
                            color = if (selected) EvColors.Primary else EvColors.OnSurfaceVar,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
                        )
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            // ── Primary EV Optional ───────────────────────────────────────
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Primary EV (Optional)", style = MaterialTheme.typography.labelMedium, color = EvColors.OnSurfaceVar)
                Text("Autoconfigures plug types", style = MaterialTheme.typography.labelSmall, color = EvColors.Primary)
            }
            Spacer(Modifier.height(10.dp))

            // Scrollable EV model chips
            Row(
                Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .background(EvColors.SurfaceHigh)
                    .border(1.dp, EvColors.SurfaceBorder, RoundedCornerShape(14.dp))
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.ElectricCar, null, tint = EvColors.Primary, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        selectedModel ?: "Tesla Model 3 / Y (NACS)",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (selectedModel != null) EvColors.OnBackground else EvColors.OnSurfaceVar
                    )
                    Text("Up to 250 kW DC Fast Charging", style = MaterialTheme.typography.labelSmall, color = EvColors.OnSurfaceVar)
                }
                Checkbox(
                    checked = selectedModel != null,
                    onCheckedChange = { selectedModel = if (it) evModels[0] else null },
                    colors = CheckboxDefaults.colors(checkedColor = EvColors.Primary)
                )
            }
            Spacer(Modifier.height(8.dp))
            // Quick-select chips
            @Suppress("UNCHECKED_CAST")
            val extras = evModels.drop(1).take(3)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                extras.forEach { model ->
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(50))
                            .background(EvColors.SurfaceHigh)
                            .border(1.dp, EvColors.SurfaceBorder, RoundedCornerShape(50))
                            .clickable { selectedModel = model }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(model, style = MaterialTheme.typography.labelSmall, color = EvColors.OnSurfaceVar)
                    }
                }
                Box(
                    Modifier
                        .clip(RoundedCornerShape(50))
                        .background(EvColors.SurfaceHigh)
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text("+ O", style = MaterialTheme.typography.labelSmall, color = EvColors.OnSurfaceVar)
                }
            }

            Spacer(Modifier.height(20.dp))

            // ── Terms ─────────────────────────────────────────────────────
            Row(verticalAlignment = Alignment.Top) {
                Checkbox(
                    checked = agreedTerms,
                    onCheckedChange = { agreedTerms = it },
                    colors = CheckboxDefaults.colors(checkedColor = EvColors.Primary)
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    buildAnnotatedString {
                        withStyle(SpanStyle(color = EvColors.OnSurfaceVar)) { append("I agree to the ") }
                        withStyle(SpanStyle(color = EvColors.Primary)) { append("Terms of Service") }
                        withStyle(SpanStyle(color = EvColors.OnSurfaceVar)) { append(", ") }
                        withStyle(SpanStyle(color = EvColors.Primary)) { append("Privacy Policy") }
                        withStyle(SpanStyle(color = EvColors.OnSurfaceVar)) { append(", and smart charging notifications.") }
                    },
                    style = MaterialTheme.typography.bodySmall,
                    lineHeight = 18.sp
                )
            }

            Spacer(Modifier.height(8.dp))

            // ── Error ─────────────────────────────────────────────────────
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

            Spacer(Modifier.height(12.dp))

            // ── CTA ───────────────────────────────────────────────────────
            EvPrimaryButton(
                text = "Create Account & Get Started",
                onClick = { viewModel.register(name.trim(), email.trim(), password, role) },
                modifier = Modifier.fillMaxWidth(),
                enabled = !state.loading && name.isNotBlank() && email.isNotBlank() && password.length >= 6 && agreedTerms,
                loading = state.loading,
                icon = Icons.Filled.Bolt
            )

            Spacer(Modifier.height(16.dp))

            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                TextButton(onClick = onBackToLogin) {
                    Text(
                        buildAnnotatedString {
                            withStyle(SpanStyle(color = EvColors.OnSurfaceVar)) { append("Already have an account? ") }
                            withStyle(SpanStyle(color = EvColors.Primary, fontWeight = FontWeight.Bold)) { append("Sign In") }
                        }
                    )
                }
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Filled.Shield, null, tint = EvColors.Primary, modifier = Modifier.size(13.dp))
                    Text("Free tier forever • No credit card required to explore", style = MaterialTheme.typography.labelSmall, color = EvColors.OnSurfaceVar)
                }
            }

            Spacer(Modifier.height(20.dp))

            // ── Bottom promo card ─────────────────────────────────────────
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(EvColors.Surface)
                    .border(1.dp, EvColors.SurfaceBorder, RoundedCornerShape(14.dp))
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(EvColors.PrimaryDim),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.EvStation, null, tint = EvColors.Primary, modifier = Modifier.size(20.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text("Reserve charging stalls ahead", style = MaterialTheme.typography.titleSmall, color = EvColors.OnBackground, fontWeight = FontWeight.SemiBold)
                    Text("Lock a 350kW ultra-rapid dispenser up to 25\nminutes prior to arrival.", style = MaterialTheme.typography.bodySmall, color = EvColors.OnSurfaceVar, lineHeight = 17.sp)
                }
            }
        }
    }
}
