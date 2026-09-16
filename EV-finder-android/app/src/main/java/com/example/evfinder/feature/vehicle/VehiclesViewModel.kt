package com.example.evfinder.feature.vehicle

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.evfinder.core.model.VehicleDto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class VehiclesUiState(
    val loading: Boolean = true,
    val vehicles: List<VehicleDto> = emptyList(),
    val error: String? = null,
    // add/edit dialog: null = closed, VehicleDto = editing existing
    val editing: VehicleDto? = null,
    val showAdd: Boolean = false,
    val saving: Boolean = false,
    val deletingId: String? = null
)

class VehiclesViewModel : ViewModel() {

    private val repository = VehiclesRepository()

    private val _uiState = MutableStateFlow(VehiclesUiState())
    val uiState: StateFlow<VehiclesUiState> = _uiState

    init { load() }

    fun load() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(loading = true, error = null)
            repository.myVehicles().fold(
                onSuccess = { v -> _uiState.value = _uiState.value.copy(loading = false, vehicles = v) },
                onFailure = { e -> _uiState.value = _uiState.value.copy(loading = false, error = e.message) }
            )
        }
    }

    fun openAdd() { _uiState.value = _uiState.value.copy(showAdd = true, editing = null) }
    fun openEdit(vehicle: VehicleDto) { _uiState.value = _uiState.value.copy(showAdd = true, editing = vehicle) }
    fun closeDialog() { _uiState.value = _uiState.value.copy(showAdd = false, editing = null) }

    fun save(
        vehicleType: String, registrationNo: String,
        manufacturer: String, model: String, connectorType: String, batteryKwh: String
    ) {
        val editing = _uiState.value.editing
        val kwh = batteryKwh.toDoubleOrNull()
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(saving = true, error = null)
            val result = if (editing != null) {
                repository.updateVehicle(editing.id, vehicleType, registrationNo,
                    manufacturer.takeIf { it.isNotBlank() }, model.takeIf { it.isNotBlank() },
                    connectorType.takeIf { it.isNotBlank() }, kwh)
            } else {
                repository.addVehicle(vehicleType, registrationNo,
                    manufacturer.takeIf { it.isNotBlank() }, model.takeIf { it.isNotBlank() },
                    connectorType.takeIf { it.isNotBlank() }, kwh)
            }
            result.fold(
                onSuccess = {
                    _uiState.value = _uiState.value.copy(saving = false, showAdd = false, editing = null)
                    load()
                },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(saving = false, error = e.message)
                }
            )
        }
    }

    fun delete(id: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(deletingId = id, error = null)
            repository.deleteVehicle(id).fold(
                onSuccess = {
                    _uiState.value = _uiState.value.copy(
                        deletingId = null,
                        vehicles = _uiState.value.vehicles.filter { it.id != id }
                    )
                },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(deletingId = null, error = e.message)
                }
            )
        }
    }
}
