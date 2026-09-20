package com.example.evfinder.feature.booking

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.EvStation
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.evfinder.core.model.BookingDto
import com.example.evfinder.ui.components.EvPrimaryButton
import com.example.evfinder.ui.components.EvProgressBar
import com.example.evfinder.ui.components.EvStepBar
import com.example.evfinder.ui.components.EvTextField
import com.example.evfinder.ui.components.LabeledDivider
import com.example.evfinder.ui.components.StatusPill
import com.example.evfinder.ui.theme.EvColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class PaymentUiState(
    val booking: BookingDto? = null,
    val loadingBooking: Boolean = true,
    val processing: Boolean = false,
    val processingStep: Int = 0,
    val error: String? = null,
    val result: PaymentResult? = null
)

data class PaymentResult(val success: Boolean, val message: String, val reference: String?)

class PaymentViewModel(private val bookingId: String) : ViewModel() {

    private val repository = BookingRepository()

    private val _uiState = MutableStateFlow(PaymentUiState())
    val uiState: StateFlow<PaymentUiState> = _uiState

    init { loadBooking() }

    private fun loadBooking() {
        viewModelScope.launch {
            repository.booking(bookingId).fold(
                onSuccess = { _uiState.value = _uiState.value.copy(loadingBooking = false, booking = it) },
                onFailure = { e -> _uiState.value = _uiState.value.copy(loadingBooking = false, error = e.message) }
            )
        }
    }

    fun pay(method: String, forceFailure: Boolean) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(processing = true, processingStep = 0, error = null)
            val steps = 3
            for (i in 0 until steps) {
                _uiState.value = _uiState.value.copy(processingStep = i)
                delay(650)
            }
            repository.pay(bookingId, method, forceFailure).fold(
                onSuccess = { p ->
                    val success = p.status == "SUCCESS"
                    _uiState.value = _uiState.value.copy(
                        processing = false,
                        result = PaymentResult(
                            success = success,
                            message = if (success)
                                "Your booking is confirmed. Present this reference at the station."
                            else "Payment failed — the slot was released. You can book again.",
                            reference = p.transactionRef
                        )
                    )
                },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(processing = false, error = e.message)
                }
            )
        }
    }
}

/**
 * Simulated payment — Voltage Mobility layout: 6-step progress bar, secure
 * payment card with quick-pay options, card form, order summary driven by the
 * real booking, and a demo switch to exercise the failure path.
 */
@Composable
fun PaymentScreen(
    bookingId: String,
    onDone: (bookingId: String) -> Unit,
    onBack: () -> Unit = { onDone(bookingId) }
) {
    val vm: PaymentViewModel = viewModel(
        factory = viewModelFactory { initializer { PaymentViewModel(bookingId) } }
    )
    val state by vm.uiState.collectAsState()

    var cardName by remember { mutableStateOf("") }
    var cardNumber by remember { mutableStateOf("") }
    var expiry by remember { mutableStateOf("") }
    var cvc by remember { mutableStateOf("") }
    var forceFailure by remember { mutableStateOf(false) }

    val booking = state.booking
    val amount = booking?.amount ?: 0.0
    val amountText = amount.toBigDecimal().stripTrailingZeros().toPlainString()

    Column(
        Modifier
            .fillMaxSize()
            .background(EvColors.Background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // back button — the payment flow is a pushed screen, always reversible
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(EvColors.Surface)
                    .clickable(onClick = onBack),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back",
                    tint = EvColors.OnSurface, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column {
                Text("Payment", style = MaterialTheme.typography.headlineSmall,
                    color = EvColors.OnBackground, fontWeight = FontWeight.Bold)
                Text("Complete payment to confirm your slot.",
                    style = MaterialTheme.typography.labelSmall, color = EvColors.OnSurfaceVar)
            }
        }

        Spacer(Modifier.height(18.dp))
        // STEP 5 OF 6 progress header (mockup)
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("STEP 5 OF 6", style = MaterialTheme.typography.labelSmall,
                color = EvColors.Primary, letterSpacing = 1.5.sp,
                modifier = Modifier.weight(1f))
            Text("Payment Confirmation", style = MaterialTheme.typography.labelSmall,
                color = EvColors.OnSurfaceVar)
        }
        Spacer(Modifier.height(8.dp))
        EvProgressBar(fraction = 5f / 6f, height = 6.dp)
        Spacer(Modifier.height(24.dp))

        // ── result ──
        state.result?.let { result ->
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (result.success) EvColors.Primary.copy(alpha = 0.12f)
                        else EvColors.ErrorContainer.copy(alpha = 0.35f)
                    )
                    .border(
                        1.dp,
                        if (result.success) EvColors.Primary.copy(alpha = 0.4f)
                        else EvColors.Error.copy(alpha = 0.4f),
                        RoundedCornerShape(12.dp)
                    )
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(if (result.success) EvColors.Primary else EvColors.Error),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        if (result.success) Icons.Filled.Check else Icons.Filled.Lock,
                        null,
                        tint = if (result.success) EvColors.OnPrimary else EvColors.Background,
                        modifier = Modifier.size(28.dp)
                    )
                }
                Spacer(Modifier.height(14.dp))
                Text(
                    if (result.success) "Payment Successful" else "Payment Failed",
                    style = MaterialTheme.typography.titleLarge,
                    color = EvColors.OnBackground, fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(6.dp))
                Text(result.message, style = MaterialTheme.typography.bodySmall,
                    color = EvColors.OnSurfaceVar)
                result.reference?.let {
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "Ref $it",
                        style = MaterialTheme.typography.labelLarge,
                        color = if (result.success) EvColors.Primary else EvColors.Error,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(Modifier.height(18.dp))
                EvPrimaryButton(
                    text = if (result.success) "View Booking Receipt" else "Back",
                    onClick = { onDone(bookingId) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            Spacer(Modifier.height(24.dp))
            return@Column
        }

        // ── processing ──
        if (state.processing) {
            PaymentProcessing(state.processingStep)
            return@Column
        }

        if (state.loadingBooking) {
            Box(Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = EvColors.Primary)
            }
            return@Column
        }

        // ── secure payment card ──
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(EvColors.SurfaceLow)
                .padding(20.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(EvColors.Primary.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.AccountBalanceWallet, null,
                        tint = EvColors.Primary, modifier = Modifier.size(22.dp))
                }
                Spacer(Modifier.width(12.dp))
                Text("Secure Payment", style = MaterialTheme.typography.titleLarge,
                    color = EvColors.OnBackground, fontWeight = FontWeight.Bold)
            }

            Spacer(Modifier.height(20.dp))

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                QuickPayButton("Mobile Banking", "bKash / Nagad", Modifier.weight(1f),
                    enabled = !state.processing) { vm.pay("MOBILE_BANKING", forceFailure) }
                QuickPayButton("Pay at Station", "Cash on arrival", Modifier.weight(1f),
                    enabled = !state.processing) { vm.pay("CASH_AT_STATION", forceFailure) }
            }

            Spacer(Modifier.height(20.dp))
            LabeledDivider("Or pay with card")
            Spacer(Modifier.height(20.dp))

            EvTextField(cardName, { cardName = it }, "ALEXANDER WRIGHT",
                label = "Cardholder Name", uppercase = true)
            Spacer(Modifier.height(14.dp))
            EvTextField(
                cardNumber,
                { if (it.length <= 16) cardNumber = it },
                "0000000000000000",
                label = "Card Number",
                leadingIcon = Icons.Filled.CreditCard,
                keyboardType = KeyboardType.Number
            )
            Spacer(Modifier.height(14.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(Modifier.weight(1f)) {
                    EvTextField(expiry, { if (it.length <= 5) expiry = it }, "MM / YY",
                        label = "Expiry", keyboardType = KeyboardType.Number)
                }
                Box(Modifier.weight(1f)) {
                    EvTextField(cvc, { if (it.length <= 4) cvc = it }, "•••",
                        label = "CVC", keyboardType = KeyboardType.NumberPassword)
                }
            }

            Spacer(Modifier.height(16.dp))
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(EvColors.Surface)
                    .clickable { forceFailure = !forceFailure }
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = forceFailure,
                    onCheckedChange = { forceFailure = it },
                    colors = CheckboxDefaults.colors(
                        checkedColor = EvColors.Warning,
                        uncheckedColor = EvColors.OnSurfaceVar
                    )
                )
                Column {
                    Text("Simulate payment failure",
                        style = MaterialTheme.typography.labelMedium, color = EvColors.OnSurface)
                    Text("Demo switch — exercises the slot-release path",
                        style = MaterialTheme.typography.labelSmall, color = EvColors.OnSurfaceVar)
                }
            }
        }

        state.error?.let {
            Spacer(Modifier.height(12.dp))
            Text(it, color = EvColors.Error, style = MaterialTheme.typography.bodySmall)
        }

        Spacer(Modifier.height(20.dp))

        // ── order summary ──
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(EvColors.SurfaceHighest)
                .padding(20.dp)
        ) {
            Text("Order Summary", style = MaterialTheme.typography.titleLarge,
                color = EvColors.OnSurface, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(12.dp))
            HorizontalDivider(color = EvColors.OutlineVariant)
            Spacer(Modifier.height(16.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(EvColors.SurfaceLow)
                        .border(1.dp, EvColors.OutlineVariant, RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.EvStation, null, tint = EvColors.Primary, modifier = Modifier.size(28.dp))
                }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(booking?.stationName ?: "—",
                        style = MaterialTheme.typography.titleSmall, color = EvColors.OnSurface,
                        fontWeight = FontWeight.SemiBold, maxLines = 1)
                    Text(
                        if (booking?.serviceName == "BATTERY_SWAP") "Battery swap service" else "Charging service",
                        style = MaterialTheme.typography.labelSmall, color = EvColors.OnSurfaceVar
                    )
                }
                StatusPill(label = "Reserved", isActive = true, showDot = false)
            }

            booking?.let { b ->
                Spacer(Modifier.height(16.dp))
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(EvColors.SurfaceLow)
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.Schedule, null, tint = EvColors.Primary, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(b.startTime.take(10), style = MaterialTheme.typography.labelLarge,
                            color = EvColors.OnSurface)
                        Text("${b.startTime.substring(11, 16)} – ${b.endTime.substring(11, 16)}",
                            style = MaterialTheme.typography.labelSmall, color = EvColors.OnSurfaceVar)
                    }
                    Spacer(Modifier.weight(1f))
                    Icon(Icons.Filled.Bolt, null, tint = EvColors.Secondary, modifier = Modifier.size(16.dp))
                }
            }

            Spacer(Modifier.height(16.dp))
            CostRow("Booking status", booking?.status ?: "—")
            Spacer(Modifier.height(8.dp))
            CostRow("Service", if (booking?.serviceName == "BATTERY_SWAP") "Battery Swap" else "Charging")
            Spacer(Modifier.height(8.dp))
            CostRow("Gateway", "Simulated (no real charge)")
            Spacer(Modifier.height(14.dp))
            HorizontalDivider(color = EvColors.OutlineVariant)
            Spacer(Modifier.height(14.dp))

            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
                Text("Total", style = MaterialTheme.typography.titleLarge, color = EvColors.OnSurface,
                    modifier = Modifier.weight(1f))
                Text("৳$amountText", style = MaterialTheme.typography.headlineMedium,
                    color = EvColors.Primary, fontWeight = FontWeight.Bold)
            }

            Spacer(Modifier.height(18.dp))
            EvPrimaryButton(
                text = "Pay ৳$amountText Now",
                onClick = { vm.pay("CARD", forceFailure) },
                enabled = !state.processing,
                icon = Icons.Filled.Lock,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(14.dp))
            Row(
                Modifier.fillMaxWidth().alpha(0.7f),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.Lock, null, tint = EvColors.OnSurfaceVar, modifier = Modifier.size(12.dp))
                Spacer(Modifier.width(6.dp))
                Text("Simulated payment — no real money is charged",
                    style = MaterialTheme.typography.labelSmall, color = EvColors.OnSurfaceVar)
            }
        }

        Spacer(Modifier.height(32.dp))
    }
}

@Composable
private fun QuickPayButton(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Column(
        modifier
            .height(72.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(EvColors.Surface)
            .border(1.dp, EvColors.OutlineVariant, RoundedCornerShape(8.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 12.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text(title, style = MaterialTheme.typography.labelLarge, color = EvColors.OnSurface,
            fontWeight = FontWeight.SemiBold)
        Text(subtitle, style = MaterialTheme.typography.labelSmall, color = EvColors.OnSurfaceVar)
    }
}

@Composable
private fun CostRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = EvColors.OnSurfaceVar,
            modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.labelLarge, color = EvColors.OnSurface)
    }
}

@Composable
private fun PaymentProcessing(step: Int) {
    val steps = listOf("Contacting payment gateway…", "Verifying payment method…", "Confirming your booking…")
    val pulse = rememberInfiniteTransition(label = "proc")
    val a by pulse.animateFloat(0.4f, 1f, infiniteRepeatable(tween(800), RepeatMode.Reverse), label = "a")

    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(EvColors.SurfaceLow)
            .padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(EvColors.Primary.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(
                Modifier.size(44.dp).alpha(a), color = EvColors.Primary, strokeWidth = 3.dp
            )
        }
        Spacer(Modifier.height(18.dp))
        AnimatedContent(targetState = step, label = "stepText") { s ->
            Text(steps[s.coerceIn(0, steps.lastIndex)],
                style = MaterialTheme.typography.bodyMedium, color = EvColors.OnSurface)
        }
        Spacer(Modifier.height(18.dp))
        steps.forEachIndexed { index, label ->
            Row(
                Modifier.fillMaxWidth().padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    if (index < step) Icons.Filled.Check else Icons.Filled.Schedule,
                    null,
                    tint = if (index < step) EvColors.Primary else EvColors.OnSurfaceVar,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    label,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (index < step) EvColors.OnSurface else EvColors.OnSurfaceVar
                )
            }
        }
    }
}
