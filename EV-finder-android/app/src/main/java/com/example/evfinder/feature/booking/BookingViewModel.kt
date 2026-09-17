package com.example.evfinder.feature.booking

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.evfinder.core.model.BookingDto
import com.example.evfinder.core.model.ServiceDto
import com.example.evfinder.core.model.SlotDto
import com.example.evfinder.core.model.VehicleDto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

data class BookingUiState(
    val loading: Boolean = true,
    val service: ServiceDto? = null,   // price/type for the cost estimator
    val vehicles: List<VehicleDto> = emptyList(),
    val selectedVehicleId: String? = null,
    val dates: List<LocalDate> = emptyList(),
    val selectedDate: LocalDate = LocalDate.now(),
    val slots: List<SlotDto> = emptyList(),
    val selectedSlot: SlotDto? = null,
    val slotsLoading: Boolean = false,
    val submitting: Boolean = false,
    val error: String? = null,
    val createdBooking: BookingDto? = null
)

class BookingViewModel(private val serviceId: String) : ViewModel() {

    private val repository = BookingRepository()

    private val _uiState = MutableStateFlow(
        BookingUiState(dates = (0..6).map { LocalDate.now().plusDays(it.toLong()) })
    )
    val uiState: StateFlow<BookingUiState> = _uiState

    init {
        loadService()
        loadVehicles()
        loadSlots()
    }

    fun selectVehicle(id: String) {
        _uiState.value = _uiState.value.copy(selectedVehicleId = id)
    }

    fun selectDate(date: LocalDate) {
        _uiState.value = _uiState.value.copy(selectedDate = date, selectedSlot = null)
        loadSlots()
    }

    fun selectSlot(slot: SlotDto) {
        _uiState.value = _uiState.value.copy(selectedSlot = slot)
    }

    fun addVehicle(vehicleType: String, registrationNo: String, batteryKwh: Double? = null) {
        viewModelScope.launch {
            repository.addVehicle(vehicleType, registrationNo, null, null, null, batteryKwh).fold(
                onSuccess = { loadVehicles(it.id) },
                onFailure = { e -> _uiState.value = _uiState.value.copy(error = e.message) }
            )
        }
    }

    fun submitBooking() {
        val s = _uiState.value
        val vehicleId = s.selectedVehicleId ?: return
        val slot = s.selectedSlot ?: return
        viewModelScope.launch {
            _uiState.value = s.copy(submitting = true, error = null)
            repository.createBooking(vehicleId, serviceId, slot.startTime, slot.endTime).fold(
                onSuccess = { booking ->
                    _uiState.value = _uiState.value.copy(submitting = false, createdBooking = booking)
                },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(submitting = false, error = e.message)
                }
            )
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    /** Service price/type feed the booking screen's cost estimator; failure is non-fatal. */
    private fun loadService() {
        viewModelScope.launch {
            repository.service(serviceId).fold(
                onSuccess = { sv -> _uiState.value = _uiState.value.copy(service = sv) },
                onFailure = { /* estimator simply stays hidden */ }
            )
        }
    }

    private fun loadVehicles(selectFirst: String? = null) {
        viewModelScope.launch {
            repository.myVehicles().fold(
                onSuccess = { vehicles ->
                    _uiState.value = _uiState.value.copy(
                        loading = false,
                        vehicles = vehicles,
                        selectedVehicleId = selectFirst
                            ?: vehicles.firstOrNull()?.id
                    )
                },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(loading = false, error = e.message)
                }
            )
        }
    }

    private fun loadSlots() {
        val date = _uiState.value.selectedDate.format(DateTimeFormatter.ISO_LOCAL_DATE)
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(slotsLoading = true)
            repository.slots(serviceId, date).fold(
                onSuccess = { slots ->
                    _uiState.value = _uiState.value.copy(slotsLoading = false, slots = slots)
                },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(slotsLoading = false, slots = emptyList(), error = e.message)
                }
            )
        }
    }
}
