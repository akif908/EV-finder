package com.example.evfinder.feature.operator

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.evfinder.EvFinderApp
import com.example.evfinder.core.model.BookingDto
import com.example.evfinder.core.model.OperatorServiceRequest
import com.example.evfinder.core.model.OperatorStationRequest
import com.example.evfinder.core.model.ServiceDto
import com.example.evfinder.core.model.StationDto
import com.example.evfinder.feature.station.StationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate

data class OperatorDashboardUiState(
    val loading: Boolean = true,
    val operatorName: String = "Operator",
    val stations: List<StationDto> = emptyList(),
    val bookings: List<BookingDto> = emptyList(),
    val activeStations: Int = 0,
    val totalStations: Int = 0,
    val todaysBookings: Int = 0,
    val revenue: Double = 0.0,
    val utilizationPct: Int = 0,
    val error: String? = null
)

class OperatorDashboardViewModel : ViewModel() {
    private val repository = OperatorRepository()
    private val _uiState = MutableStateFlow(OperatorDashboardUiState())
    val uiState: StateFlow<OperatorDashboardUiState> = _uiState

    init { load() }

    fun load() {
        viewModelScope.launch {
            var stations: List<StationDto> = emptyList()
            var bookings: List<BookingDto> = emptyList()
            var error: String? = null
            repository.myStations().fold(onSuccess = { stations = it }, onFailure = { error = it.message })
            if (error == null) repository.stationBookings().fold(onSuccess = { bookings = it }, onFailure = { error = it.message })
            val today = LocalDate.now().toString()
            val todays = bookings.filter { it.startTime.startsWith(today) && it.status != "CANCELLED" }
            val revenue = bookings.filter { it.status == "CONFIRMED" || it.status == "COMPLETED" }.sumOf { it.amount ?: 0.0 }
            val capacity = stations.sumOf { s -> s.services.sumOf { it.availableSlots } }.coerceAtLeast(1)
            val utilization = ((todays.size.toDouble() / capacity) * 100).toInt().coerceIn(0, 100)
            _uiState.value = OperatorDashboardUiState(
                loading = false,
                operatorName = EvFinderApp.instance.tokenStore.name ?: "Operator",
                stations = stations,
                bookings = bookings,
                activeStations = stations.count { it.status == "ACTIVE" },
                totalStations = stations.size,
                todaysBookings = todays.size,
                revenue = revenue,
                utilizationPct = utilization,
                error = error
            )
        }
    }
}

data class OperatorStationsUiState(
    val loading: Boolean = true,
    val stations: List<StationDto> = emptyList(),
    val myStationIds: Set<String> = emptySet(),
    val error: String? = null,
    val stationForm: StationDto? = null,
    val stationFormIsNew: Boolean = false,
    val saving: Boolean = false,
    val pickingLocation: Boolean = false,
    val pendingLat: Double? = null,
    val pendingLng: Double? = null,
    val serviceDialogStationId: String? = null,
    val serviceForm: ServiceDto? = null,
    val serviceFormIsNew: Boolean = false,
    val statusDialogStationId: String? = null
)

class OperatorStationsViewModel : ViewModel() {
    private val repository = OperatorRepository()
    private val allStationsRepo = StationRepository()
    private val _uiState = MutableStateFlow(OperatorStationsUiState())
    val uiState: StateFlow<OperatorStationsUiState> = _uiState

    init { load() }

    fun load() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(loading = true, error = null)
            var mine: List<StationDto> = emptyList()
            var all: List<StationDto> = emptyList()
            var error: String? = null
            repository.myStations().fold(onSuccess = { mine = it }, onFailure = { error = it.message })
            if (error == null) allStationsRepo.getStations().fold(onSuccess = { all = it }, onFailure = { e -> error = e.message })
            if (error == null) {
                val myIds = mine.map { it.id }.toSet()
                _uiState.value = _uiState.value.copy(
                    loading = false,
                    stations = (mine + all.filter { it.id !in myIds }).distinctBy { it.id },
                    myStationIds = myIds
                )
            } else {
                _uiState.value = _uiState.value.copy(loading = false, error = error)
            }
        }
    }

    fun openCreate() { _uiState.value = _uiState.value.copy(pickingLocation = true) }
    fun openEdit(station: StationDto) { _uiState.value = _uiState.value.copy(stationForm = station, stationFormIsNew = false) }
    fun closeForms() {
        _uiState.value = _uiState.value.copy(
            stationForm = null, stationFormIsNew = false, pickingLocation = false,
            pendingLat = null, pendingLng = null, serviceDialogStationId = null,
            serviceForm = null, serviceFormIsNew = false
        )
    }
    fun confirmLocation(lat: Double, lng: Double) {
        _uiState.value = _uiState.value.copy(pickingLocation = false, pendingLat = lat, pendingLng = lng, stationForm = null, stationFormIsNew = true)
    }
    fun cancelLocationPicker() { _uiState.value = _uiState.value.copy(pickingLocation = false) }
    fun openStatusDialog(id: String) { _uiState.value = _uiState.value.copy(statusDialogStationId = id) }
    fun closeStatusDialog() { _uiState.value = _uiState.value.copy(statusDialogStationId = null) }
    fun openServiceDialog(stationId: String, service: ServiceDto?, isNew: Boolean) {
        _uiState.value = _uiState.value.copy(serviceDialogStationId = stationId, serviceForm = service, serviceFormIsNew = isNew)
    }

    fun saveStation(name: String, description: String, address: String, open: String, close: String) {
        val s = _uiState.value
        val lat = s.pendingLat ?: s.stationForm?.latitude
        val lng = s.pendingLng ?: s.stationForm?.longitude
        if (lat == null || lng == null) {
            _uiState.value = s.copy(error = "Pick the station location on the map first")
            return
        }
        val body = OperatorStationRequest(name.trim(), description.trim().ifBlank { null }, address.trim().ifBlank { null }, lat, lng, open.ifBlank { null }, close.ifBlank { null })
        viewModelScope.launch {
            _uiState.value = s.copy(saving = true, error = null)
            val result = if (s.stationFormIsNew) repository.createStation(body) else repository.updateStation(s.stationForm!!.id, body)
            result.fold(
                onSuccess = {
                    _uiState.value = _uiState.value.copy(saving = false, stationForm = null, stationFormIsNew = false, pendingLat = null, pendingLng = null)
                    load()
                },
                onFailure = { e -> _uiState.value = _uiState.value.copy(saving = false, error = e.message) }
            )
        }
    }

    fun changeStatus(stationId: String, status: String) {
        viewModelScope.launch {
            repository.setStatus(stationId, status).fold(
                onSuccess = { _uiState.value = _uiState.value.copy(statusDialogStationId = null); load() },
                onFailure = { e -> _uiState.value = _uiState.value.copy(error = e.message) }
            )
        }
    }

    fun saveService(stationId: String, serviceType: String, connector: String, powerKw: String, price: String, slots: String) {
        val s = _uiState.value
        val body = OperatorServiceRequest(
            serviceType, connector.trim().ifBlank { null },
            if (serviceType == "CHARGING") powerKw.toDoubleOrNull() else null,
            price.toDoubleOrNull() ?: return fail("Price must be a number"),
            slots.toIntOrNull() ?: return fail("Slots must be a whole number")
        )
        viewModelScope.launch {
            _uiState.value = s.copy(saving = true, error = null)
            val result = if (s.serviceFormIsNew) repository.addService(stationId, body) else repository.updateService(stationId, s.serviceForm!!.id, body)
            result.fold(
                onSuccess = {
                    _uiState.value = _uiState.value.copy(saving = false, serviceDialogStationId = null, serviceForm = null, serviceFormIsNew = false)
                    load()
                },
                onFailure = { e -> _uiState.value = _uiState.value.copy(saving = false, error = e.message) }
            )
        }
    }

    fun deleteService(stationId: String, serviceId: String) {
        viewModelScope.launch {
            repository.deleteService(stationId, serviceId).fold(onSuccess = { load() }, onFailure = { e -> _uiState.value = _uiState.value.copy(error = e.message) })
        }
    }

    private fun fail(msg: String) { _uiState.value = _uiState.value.copy(error = msg) }
}

data class OperatorBookingsUiState(
    val loading: Boolean = true,
    val bookings: List<BookingDto> = emptyList(),
    val error: String? = null
)

class OperatorBookingsViewModel : ViewModel() {
    private val repository = OperatorRepository()
    private val _uiState = MutableStateFlow(OperatorBookingsUiState())
    val uiState: StateFlow<OperatorBookingsUiState> = _uiState
    init { load() }
    fun load() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(loading = true, error = null)
            repository.stationBookings().fold(
                onSuccess = { b -> _uiState.value = _uiState.value.copy(loading = false, bookings = b) },
                onFailure = { e -> _uiState.value = _uiState.value.copy(loading = false, error = e.message) }
            )
        }
    }
}
