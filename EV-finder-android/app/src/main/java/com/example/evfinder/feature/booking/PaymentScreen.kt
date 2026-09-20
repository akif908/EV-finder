@file:OptIn(ExperimentalLayoutApi::class)

package com.example.evfinder.feature.booking

import androidx.compose.animation.core.*
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.evfinder.feature.auth.EvTextField
import com.example.evfinder.ui.components.*
import com.example.evfinder.ui.theme.EvColors
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class PaymentUiState(
    val processing: Boolean = false,
    val error: String? = null,
    val result: PaymentResult? = null
)

data class PaymentResult(val success: Boolean, val message: String)

class PaymentViewModel(private val bookingId: String) : ViewModel() {
    private val repository = BookingRepository()
    private val _uiState = MutableStateFlow(PaymentUiState())
    val uiState: StateFlow<PaymentUiState> = _uiState

    fun pay(method: String, forceFailure: Boolean) {
        viewModelScope.launch {
            _uiState.value = PaymentUiState(processing = true)
            repository.pay(bookingId, method, forceFailure).fold(
                onSuccess = { p ->
                    val success = p.status == "SUCCESS"
                    _uiState.value = PaymentUiState(
                        result = PaymentResult(
                            success = success,
                            message = if (success)
                                "Booking confirmed! Transaction ${p.transactionRef}"
                            else "Payment failed — the slot was released. Try booking again."
                        )
                    )
                },
                onFailure = { e -> _uiState.value = PaymentUiState(error = e.message) }
            )
        }
    }
}

@Composable
fun PaymentScreen(
    bookingId: String,
    onBack: () -> Unit,
    onDone: () -> Unit
) {
    val vm: PaymentViewModel = viewModel(
        factory = viewModelFactory { initializer { PaymentViewModel(bookingId) } }
    )
    val state by vm.uiState.collectAsState()
    var method by remember { mutableStateOf("CARD") }
    var forceFailure by remember { mutableStateOf(false) }
    var cardName by remember { mutableStateOf("") }
    var cardNumber by remember { mutableStateOf("") }
    var expiry by remember { mutableStateOf("") }
    var cvv by remember { mutableStateOf("") }
    var saveCard by remember { mutableStateOf(true) }

    Column(
        Modifier
            .fillMaxSize()
            .background(EvColors.Background)
            .verticalScroll(rememberScrollState())
    ) {
        // ── App bar ───────────────────────────────────────────────────────
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(EvColors.SurfaceHigh)
                    .border(1.dp, EvColors.SurfaceBorder, RoundedCornerShape(10.dp))
                    .clickable(enabled = !state.processing && state.result == null, onClick = onBack),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Filled.ArrowBack,
                    null,
                    tint = if (state.processing || state.result != null) EvColors.OnSurface.copy(0.4f) else EvColors.OnSurface,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(Modifier.width(12.dp))
            Text("Payment", style = MaterialTheme.typography.titleLarge, color = EvColors.OnBackground, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            Box(
                Modifier.size(34.dp).clip(RoundedCornerShape(50)).background(EvColors.PrimaryDim),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.Person, null, tint = EvColors.Primary, modifier = Modifier.size(20.dp))
            }
        }

        Column(Modifier.padding(horizontal = 16.dp)) {
            // ── Step progress ─────────────────────────────────────────────
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Text("STEP 5 OF 6", style = MaterialTheme.typography.labelSmall, color = EvColors.Primary, letterSpacing = 0.8.sp)
                Text("Payment Confirmation", style = MaterialTheme.typography.labelSmall, color = EvColors.OnSurfaceVar)
            }
            Spacer(Modifier.height(6.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                repeat(6) { idx ->
                    Box(
                        Modifier
                            .weight(1f)
                            .height(3.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(if (idx < 5) EvColors.Primary else EvColors.SurfaceBorder)
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            // ── Result overlay ────────────────────────────────────────────
            state.result?.let { result ->
                Column(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(if (result.success) EvColors.PrimaryDim else EvColors.Error.copy(0.1f))
                        .border(1.dp, if (result.success) EvColors.Primary.copy(0.4f) else EvColors.Error.copy(0.3f), RoundedCornerShape(18.dp))
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        if (result.success) Icons.Filled.CheckCircle else Icons.Filled.Cancel,
                        null,
                        tint = if (result.success) EvColors.Primary else EvColors.Error,
                        modifier = Modifier.size(52.dp)
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        if (result.success) "Payment Successful" else "Payment Failed",
                        style = MaterialTheme.typography.titleLarge,
                        color = EvColors.OnBackground,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(result.message, style = MaterialTheme.typography.bodyMedium, color = EvColors.OnSurfaceVar)
                    Spacer(Modifier.height(20.dp))
                    EvPrimaryButton(
                        "Go to My Bookings",
                        onClick = onDone,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                return@Column
            }

            // ── Processing ────────────────────────────────────────────────
            if (state.processing) {
                EvProcessingView()
                return@Column
            }

            // ── Booking summary mini-card ─────────────────────────────────
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(EvColors.Surface)
                    .border(1.dp, EvColors.SurfaceBorder, RoundedCornerShape(14.dp))
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    Modifier.size(38.dp).clip(RoundedCornerShape(10.dp)).background(EvColors.PrimaryDim),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.EvStation, null, tint = EvColors.Primary, modifier = Modifier.size(20.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("GreenPulse Hub", style = MaterialTheme.typography.titleSmall, color = EvColors.OnBackground, fontWeight = FontWeight.SemiBold)
                    Text("Oct 24 • 14:30 – 15:15 (45 min)", style = MaterialTheme.typography.bodySmall, color = EvColors.OnSurfaceVar)
                }
                Box(
                    Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(EvColors.SurfaceHigh)
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Text("Bay 04", style = MaterialTheme.typography.labelSmall, color = EvColors.OnSurface)
                }
                Spacer(Modifier.width(6.dp))
                StatusPill("Reserved", isActive = false)
            }

            Spacer(Modifier.height(10.dp))

            // EV card
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(EvColors.Surface)
                    .border(1.dp, EvColors.SurfaceBorder, RoundedCornerShape(14.dp))
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.ElectricCar, null, tint = EvColors.OnSurfaceVar, modifier = Modifier.size(32.dp))
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("Tesla Model 3", style = MaterialTheme.typography.titleSmall, color = EvColors.OnBackground, fontWeight = FontWeight.SemiBold)
                    Text("Long Range Dual Motor…", style = MaterialTheme.typography.bodySmall, color = EvColors.OnSurfaceVar)
                }
                Row(
                    Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(EvColors.Error.copy(0.1f))
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(Icons.Filled.BatteryAlert, null, tint = EvColors.Error, modifier = Modifier.size(14.dp))
                    Text("24% Current", style = MaterialTheme.typography.labelSmall, color = EvColors.Error)
                }
            }

            Spacer(Modifier.height(20.dp))

            // ── Express checkout ─────────────────────────────────────────
            Text("EXPRESS CHECKOUT", style = MaterialTheme.typography.labelSmall, color = EvColors.OnSurfaceVar, letterSpacing = 1.sp)
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                EvOutlinedButton(
                    "Apple Pay",
                    onClick = { method = "APPLE_PAY"; vm.pay(method, false) },
                    modifier = Modifier.weight(1f),
                    leadingContent = {
                        Icon(Icons.Filled.PhoneAndroid, null, tint = EvColors.OnSurface, modifier = Modifier.size(16.dp))
                    }
                )
                EvOutlinedButton(
                    "G Pay",
                    onClick = { method = "GOOGLE_PAY"; vm.pay(method, false) },
                    modifier = Modifier.weight(1f),
                    leadingContent = {
                        Icon(Icons.Filled.Public, null, tint = Color(0xFF4285F4), modifier = Modifier.size(16.dp))
                    }
                )
            }

            Spacer(Modifier.height(16.dp))
            LabeledDivider("OR PAY WITH CARD")
            Spacer(Modifier.height(16.dp))

            // ── Card form ─────────────────────────────────────────────────
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(EvColors.Surface)
                    .border(1.dp, EvColors.SurfaceBorder, RoundedCornerShape(16.dp))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Column {
                    Text("Cardholder Name", style = MaterialTheme.typography.labelMedium, color = EvColors.OnSurfaceVar)
                    Spacer(Modifier.height(6.dp))
                    EvTextField(
                        value = cardName,
                        onValueChange = { cardName = it },
                        placeholder = "Julian Alexander",
                        leadingIcon = Icons.Outlined.Person
                    )
                }
                Column {
                    Text("Card Number", style = MaterialTheme.typography.labelMedium, color = EvColors.OnSurfaceVar)
                    Spacer(Modifier.height(6.dp))
                    EvTextField(
                        value = cardNumber,
                        onValueChange = { if (it.length <= 16) cardNumber = it },
                        placeholder = "•••• •••• •••• 8842",
                        leadingIcon = Icons.Outlined.CreditCard,
                        keyboardType = KeyboardType.Number,
                        trailingIcon = Icons.Filled.CreditCard,
                        trailingTint = Color(0xFFEB001B)
                    )
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Column(Modifier.weight(1f)) {
                        Text("Expires", style = MaterialTheme.typography.labelMedium, color = EvColors.OnSurfaceVar)
                        Spacer(Modifier.height(6.dp))
                        EvTextField(
                            value = expiry,
                            onValueChange = { expiry = it },
                            placeholder = "09/28",
                            keyboardType = KeyboardType.Number
                        )
                    }
                    Column(Modifier.weight(1f)) {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("CVC / CVV", style = MaterialTheme.typography.labelMedium, color = EvColors.OnSurfaceVar)
                            Icon(Icons.Outlined.Help, null, tint = EvColors.OnSurfaceVar, modifier = Modifier.size(14.dp))
                        }
                        Spacer(Modifier.height(6.dp))
                        EvTextField(
                            value = cvv,
                            onValueChange = { if (it.length <= 4) cvv = it },
                            placeholder = "•••",
                            keyboardType = KeyboardType.NumberPassword,
                            trailingIcon = Icons.Outlined.Lock,
                            trailingTint = EvColors.OnSurfaceVar
                        )
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = saveCard,
                        onCheckedChange = { saveCard = it },
                        colors = CheckboxDefaults.colors(checkedColor = EvColors.Primary)
                    )
                    Text("Save card securely for future charges", style = MaterialTheme.typography.bodySmall, color = EvColors.OnSurface)
                }
            }

            Spacer(Modifier.height(16.dp))

            // ── Cost breakdown ────────────────────────────────────────────
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(EvColors.Surface)
                    .border(1.dp, EvColors.SurfaceBorder, RoundedCornerShape(16.dp))
                    .padding(16.dp)
            ) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Cost Breakdown", style = MaterialTheme.typography.titleSmall, color = EvColors.OnBackground, fontWeight = FontWeight.SemiBold)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(Icons.Filled.Bolt, null, tint = EvColors.Primary, modifier = Modifier.size(12.dp))
                        Text("Est. +48 kWh", style = MaterialTheme.typography.labelSmall, color = EvColors.Primary)
                    }
                }
                Spacer(Modifier.height(12.dp))
                CostRow("Reservation Fee", "$2.50")
                Spacer(Modifier.height(8.dp))
                CostRow("Est. Charging Cost", "$17.60", sublabel = "Tier 1")
                Spacer(Modifier.height(8.dp))
                CostRow("Tax & Regulatory Fees", "$1.71")
                Spacer(Modifier.height(12.dp))
                HorizontalDivider(color = EvColors.SurfaceBorder)
                Spacer(Modifier.height(12.dp))
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text("Total Amount", style = MaterialTheme.typography.titleSmall, color = EvColors.OnBackground, fontWeight = FontWeight.SemiBold)
                        Text("Holds applied upon session start", style = MaterialTheme.typography.labelSmall, color = EvColors.OnSurfaceVar)
                    }
                    Text("$21.81", style = MaterialTheme.typography.headlineMedium, color = EvColors.Primary, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(Modifier.height(12.dp))

            // ── Demo failure toggle ───────────────────────────────────────
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(EvColors.SurfaceHigh)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = forceFailure,
                    onCheckedChange = { forceFailure = it },
                    colors = CheckboxDefaults.colors(checkedColor = EvColors.Warning)
                )
                Text("Simulate payment failure (demo)", style = MaterialTheme.typography.bodySmall, color = EvColors.OnSurfaceVar)
            }

            state.error?.let {
                Spacer(Modifier.height(8.dp))
                Text(it, color = EvColors.Error, style = MaterialTheme.typography.bodySmall)
            }

            Spacer(Modifier.height(16.dp))

            // ── Pay CTA ───────────────────────────────────────────────────
            EvPrimaryButton(
                text = "Pay & Reserve Bay ($21.81)",
                onClick = { vm.pay("CARD", forceFailure) },
                modifier = Modifier.fillMaxWidth(),
                enabled = !state.processing,
                loading = state.processing,
                icon = Icons.Filled.Lock
            )

            Spacer(Modifier.height(12.dp))

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.Shield, null, tint = EvColors.Primary, modifier = Modifier.size(12.dp))
                Spacer(Modifier.width(6.dp))
                Text("ENCRYPTED 256-BIT SECURE CHECKOUT", style = MaterialTheme.typography.labelSmall, color = EvColors.OnSurfaceVar, letterSpacing = 0.5.sp)
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun CostRow(label: String, amount: String, sublabel: String? = null) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(label, style = MaterialTheme.typography.bodyMedium, color = EvColors.OnSurface)
            sublabel?.let {
                Box(
                    Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(EvColors.SurfaceHigh)
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(it, style = MaterialTheme.typography.labelSmall, color = EvColors.OnSurfaceVar)
                }
            }
        }
        Text(amount, style = MaterialTheme.typography.bodyMedium, color = EvColors.OnBackground, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun EvProcessingView() {
    val inf = rememberInfiniteTransition(label = "proc")
    val a by inf.animateFloat(0.3f, 1f, infiniteRepeatable(tween(700), RepeatMode.Reverse), label = "proc_a")
    Box(
        Modifier
            .fillMaxWidth()
            .padding(40.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Filled.Bolt, null, tint = EvColors.Primary.copy(alpha = a), modifier = Modifier.size(52.dp))
            Spacer(Modifier.height(14.dp))
            Text("Processing payment…", color = EvColors.OnSurfaceVar, style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(4.dp))
            Text("Please don't close this screen", style = MaterialTheme.typography.labelSmall, color = EvColors.OnSurfaceVar.copy(0.5f))
        }
    }
}
