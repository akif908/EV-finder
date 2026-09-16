package com.example.evfinder.feature.operator

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.evfinder.core.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

// ─── Dashboard ────────────────────────────────────────────────────────────────
data class OperatorDashboardUiState(
    val loading: Boolean = true,
    val operatorName: String = "Operator",
    val stations: List<StationDto> = emptyList(),
    val bookings: List<BookingDto> = emptyList(),
    // computed stats
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
            repository.myStations().fold(
                onSuccess = { stations = it },
                onFailure = { error = it.message }
            )
            if (error == null) {
                repository.stationBookings().fold(
                    onSuccess = { bookings = it },
                    onFailure = { error = it.message }
                )
            }

            val today = java.time.LocalDate.now().toString()
            val todays = bookings.filter {
                it.startTime.startsWith(today) && it.status != "CANCELLED"
            }
            // paid revenue: confirmed/completed bookings count as collected
            val revenue = bookings
                .filter { it.status == "CONFIRMED" || it.status == "COMPLETED" }
                .sumOf { it.amount ?: 0.0 }
            // rough utilization: today's active bookings vs concurrent capacity
            val capacity = stations.sumOf { s -> s.services.sumOf { it.availableSlots } }.coerceAtLeast(1)
            val utilization = ((todays.size.toDouble() / capacity) * 100).toInt().coerceIn(0, 100)

            _uiState.value = OperatorDashboardUiState(
                loading = false,
                operatorName = com.example.evfinder.EvFinderApp.instance.tokenStore.name ?: "Operator",
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

// ─── Stations management ──────────────────────────────────────────────────────
data class OperatorStationsUiState(
    val loading: Boolean = true,
    /** Full network (all ACTIVE stations) — owned ones are manageable, others read-only. */
    val stations: List<StationDto> = emptyList(),
    /** IDs of stations this operator owns (manage permission comes from the backend too). */
    val myStationIds: Set<String> = emptySet(),
    val error: String? = null,
    // null = closed; "new" = create; else editing this station
    val stationForm: StationDto? = null,
    val stationFormIsNew: Boolean = false,
    val saving: Boolean = false,
    // Uber-style flow: pick location on the map first, coordinates land here
    val pickingLocation: Boolean = false,
    val pendingLat: Double? = null,
    val pendingLng: Double? = null,
    // service dialog for this station id
    val serviceDialogStationId: String? = null,
    val serviceForm: ServiceDto? = null,
    val serviceFormIsNew: Boolean = false,
    val statusDialogStationId: String? = null
)

class OperatorStationsViewModel : ViewModel() {
    private val repository = OperatorRepository()
    private val allStationsRepo = com.example.evfinder.feature.station.StationRepository()

    private val _uiState = MutableStateFlow(OperatorStationsUiState())
    val uiState: StateFlow<OperatorStationsUiState> = _uiState

    init { load() }

    fun load() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(loading = true, error = null)
            var mine: List<StationDto> = emptyList()
            var all: List<StationDto> = emptyList()
            var error: String? = null
            repository.myStations().fold(
                onSuccess = { mine = it },
                onFailure = { error = it.message }
            )
            if (error == null) {
                allStationsRepo.getStations().fold(
                    onSuccess = { all = it },
                    onFailure = { e -> error = e.message }
                )
            }
            if (error == null) {
                val myIds = mine.map { it.id }.toSet()
                // full network; my stations (incl. any non-ACTIVE ones) first, then others
                val combined = (mine + all.filter { it.id !in myIds })
                    .distinctBy { it.id }
                _uiState.value = _uiState.value.copy(
                    loading = false, stations = combined, myStationIds = myIds
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
            stationForm = null, stationFormIsNew = false,
            pickingLocation = false, pendingLat = null, pendingLng = null,
            serviceDialogStationId = null, serviceForm = null, serviceFormIsNew = false
        )
    }

    /** Called by the map picker when the operator taps a spot and confirms. */
    fun confirmLocation(lat: Double, lng: Double) {
        _uiState.value = _uiState.value.copy(
            pickingLocation = false, pendingLat = lat, pendingLng = lng,
            stationForm = null, stationFormIsNew = true
        )
    }

    fun cancelLocationPicker() {
        _uiState.value = _uiState.value.copy(pickingLocation = false)
    }

    fun openStatusDialog(stationId: String) { _uiState.value = _uiState.value.copy(statusDialogStationId = stationId) }
    fun closeStatusDialog() { _uiState.value = _uiState.value.copy(statusDialogStationId = null) }

    fun saveStation(name: String, description: String, address: String, open: String, close: String, fuelLevel: Int) {
        val s = _uiState.value
        val editing = s.stationForm
        val lat = s.pendingLat ?: editing?.latitude
        val lng = s.pendingLng ?: editing?.longitude
        if (lat == null || lng == null) {
            return fail("Pick the station location on the map first")
        }
        val body = OperatorStationRequest(
            name = name.trim(),
            description = description.trim().takeIf { it.isNotBlank() },
            address = address.trim().takeIf { it.isNotBlank() },
            latitude = lat,
            longitude = lng,
            openingTime = open.takeIf { it.isNotBlank() },
            closingTime = close.takeIf { it.isNotBlank() },
            fuelLevel = fuelLevel
        )
        viewModelScope.launch {
            _uiState.value = s.copy(saving = true, error = null)
            val result = if (s.stationFormIsNew) repository.createStation(body)
            else repository.updateStation(editing!!.id, body)
            result.fold(
                onSuccess = {
                    _uiState.value = _uiState.value.copy(
                        saving = false, stationForm = null, stationFormIsNew = false,
                        pendingLat = null, pendingLng = null
                    )
                    load()
                },
                onFailure = { e -> _uiState.value = _uiState.value.copy(saving = false, error = e.message) }
            )
        }
    }

    private fun fail(msg: String) { _uiState.value = _uiState.value.copy(error = msg) }

    fun changeStatus(stationId: String, status: String) {
        viewModelScope.launch {
            repository.setStatus(stationId, status).fold(
                onSuccess = {
                    _uiState.value = _uiState.value.copy(statusDialogStationId = null)
                    load()
                },
                onFailure = { e -> _uiState.value = _uiState.value.copy(error = e.message) }
            )
        }
    }

    fun openServiceDialog(stationId: String, service: ServiceDto?, isNew: Boolean) {
        _uiState.value = _uiState.value.copy(serviceDialogStationId = stationId, serviceForm = service, serviceFormIsNew = isNew)
    }

    fun saveService(stationId: String, serviceType: String, connector: String, powerKw: String, price: String, slots: String) {
        val s = _uiState.value
        val body = OperatorServiceRequest(
            serviceType = serviceType,
            connectorType = connector.trim().takeIf { it.isNotBlank() },
            powerKw = if (serviceType == "CHARGING") powerKw.toDoubleOrNull() else null,
            pricePerUnit = price.toDoubleOrNull() ?: return fail("Price must be a number"),
            availableSlots = slots.toIntOrNull() ?: return fail("Slots must be a whole number")
        )
        viewModelScope.launch {
            _uiState.value = s.copy(saving = true, error = null)
            val result = if (s.serviceFormIsNew) repository.addService(stationId, body)
            else repository.updateService(stationId, s.serviceForm!!.id, body)
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
            repository.deleteService(stationId, serviceId).fold(
                onSuccess = { load() },
                onFailure = { e -> _uiState.value = _uiState.value.copy(error = e.message) }
            )
        }
    }
}

// ─── Station bookings ─────────────────────────────────────────────────────────
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
