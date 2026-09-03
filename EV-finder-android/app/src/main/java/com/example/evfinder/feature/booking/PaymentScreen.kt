@file:OptIn(ExperimentalLayoutApi::class)

package com.example.evfinder.feature.booking

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
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
    onDone: () -> Unit
) {
    val vm: PaymentViewModel = viewModel(
        factory = viewModelFactory { initializer { PaymentViewModel(bookingId) } }
    )
    val state by vm.uiState.collectAsState()
    var method by remember { mutableStateOf("MOBILE_BANKING") }
    var forceFailure by remember { mutableStateOf(false) }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text("Simulated Payment", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(16.dp))

        state.result?.let { result ->
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (result.success) MaterialTheme.colorScheme.primaryContainer
                    else MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Column(
                    Modifier.padding(16.dp).fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        if (result.success) "✓ Payment Successful" else "✗ Payment Failed",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(result.message, style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = onDone) { Text(if (result.success) "Go to My Bookings" else "Back") }
                }
            }
            return@Column
        }

        if (state.processing) {
            AnimatedProcessingView()
            return@Column
        }

        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Column(Modifier.padding(16.dp)) {
                Text("Choose payment method", fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(10.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        "MOBILE_BANKING" to "Mobile Banking",
                        "CARD" to "Card",
                        "CASH_AT_STATION" to "Cash at Station"
                    ).forEach { (value, label) ->
                        FilterChip(selected = method == value, onClick = { method = value }, label = { Text(label) })
                    }
                }
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = forceFailure, onCheckedChange = { forceFailure = it })
                    Text("Simulate payment failure (demo)", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        Spacer(Modifier.height(16.dp))

        state.error?.let {
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(8.dp))
        }

        Button(
            onClick = { vm.pay(method, forceFailure) },
            enabled = !state.processing,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (state.processing) CircularProgressIndicator(Modifier.height(20.dp), strokeWidth = 2.dp)
            else Text("Process Payment")
        }
    }
}
