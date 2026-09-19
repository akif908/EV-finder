package com.example.evfinder.feature.fuel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.evfinder.core.model.FuelInventoryDto
import com.example.evfinder.core.model.FuelStationDto
import com.example.evfinder.core.model.FuelStationRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class OperatorFuelUiState(
    val loading: Boolean = true,
    val stations: List<FuelStationDto> = emptyList(),
    val error: String? = null,
    val saving: Boolean = false,
    // station form: null = closed; "new" = create (after map pick); else editing this station
    val stationForm: FuelStationDto? = null,
    val stationFormIsNew: Boolean = false,
    val pickingLocation: Boolean = false,
    val pendingLat: Double? = null,
    val pendingLng: Double? = null,
    // fuel type dialog for this station id (fuelForm null = add new type)
    val fuelDialogStationId: String? = null,
    val fuelForm: FuelInventoryDto? = null,
    val confirmDelete: FuelStationDto? = null
)

class OperatorFuelViewModel : ViewModel() {

    private val repository = FuelRepository()

    private val _uiState = MutableStateFlow(OperatorFuelUiState())
    val uiState: StateFlow<OperatorFuelUiState> = _uiState

    init { load() }

    fun load() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(loading = true, error = null)
            repository.myFuelStations().fold(
                onSuccess = { stations ->
                    _uiState.value = _uiState.value.copy(loading = false, stations = stations)
                },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(loading = false, error = e.message)
                }
            )
        }
    }

    // ---- station create/edit ----

    fun openCreate() { _uiState.value = _uiState.value.copy(pickingLocation = true) }

    fun confirmLocation(lat: Double, lng: Double) {
        _uiState.value = _uiState.value.copy(
            pickingLocation = false, pendingLat = lat, pendingLng = lng,
            stationForm = null, stationFormIsNew = true
        )
    }

    fun cancelLocationPicker() { _uiState.value = _uiState.value.copy(pickingLocation = false) }

    fun openEdit(station: FuelStationDto) {
        _uiState.value = _uiState.value.copy(stationForm = station, stationFormIsNew = false)
    }

    fun closeForms() {
        _uiState.value = _uiState.value.copy(
            stationForm = null, stationFormIsNew = false,
            pickingLocation = false, pendingLat = null, pendingLng = null,
            fuelDialogStationId = null, fuelForm = null
        )
    }

    fun saveStation(name: String, description: String, address: String, isOpen: Boolean) {
        val s = _uiState.value
        val editing = s.stationForm
        val lat = s.pendingLat ?: editing?.latitude
        val lng = s.pendingLng ?: editing?.longitude
        if (lat == null || lng == null) {
            _uiState.value = s.copy(error = "Pick the station location on the map first")
            return
        }
        val body = FuelStationRequest(
            name = name.trim(),
            description = description.trim().takeIf { it.isNotBlank() },
            address = address.trim().takeIf { it.isNotBlank() },
            latitude = lat,
            longitude = lng,
            isOpen = isOpen
        )
        viewModelScope.launch {
            _uiState.value = s.copy(saving = true, error = null)
            val result = if (s.stationFormIsNew) repository.createFuelStation(body)
            else repository.updateFuelStation(editing!!.id, body)
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

    // ---- open/close, delete ----

    fun toggleOpen(station: FuelStationDto) {
        viewModelScope.launch {
            repository.setOpen(station.id, !station.isOpen).fold(
                onSuccess = { load() },
                onFailure = { e -> _uiState.value = _uiState.value.copy(error = e.message) }
            )
        }
    }

    fun askDelete(station: FuelStationDto) {
        _uiState.value = _uiState.value.copy(confirmDelete = station)
    }

    fun dismissDelete() {
        _uiState.value = _uiState.value.copy(confirmDelete = null)
    }

    fun deleteConfirmed() {
        val station = _uiState.value.confirmDelete ?: return
        viewModelScope.launch {
            repository.deleteFuelStation(station.id).fold(
                onSuccess = { _uiState.value = _uiState.value.copy(confirmDelete = null); load() },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(confirmDelete = null, error = e.message)
                }
            )
        }
    }

    // ---- per fuel type: queue / remaining liters / price ----

    fun openFuelDialog(stationId: String, inventory: FuelInventoryDto?) {
        _uiState.value = _uiState.value.copy(fuelDialogStationId = stationId, fuelForm = inventory)
    }

    fun saveFuel(stationId: String, fuelType: String, queue: Int, liters: Double, price: Double) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(saving = true, error = null)
            repository.updateFuelInventory(stationId, fuelType, queue, liters, price).fold(
                onSuccess = {
                    _uiState.value = _uiState.value.copy(saving = false, fuelDialogStationId = null, fuelForm = null)
                    load()
                },
                onFailure = { e -> _uiState.value = _uiState.value.copy(saving = false, error = e.message) }
            )
        }
    }

    fun removeFuel(stationId: String, fuelType: String) {
        viewModelScope.launch {
            repository.removeFuelType(stationId, fuelType).fold(
                onSuccess = { load() },
                onFailure = { e -> _uiState.value = _uiState.value.copy(error = e.message) }
            )
        }
    }
}
