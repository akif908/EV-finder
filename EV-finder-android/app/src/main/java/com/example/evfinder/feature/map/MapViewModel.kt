package com.example.evfinder.feature.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.evfinder.core.model.StationDto
import com.example.evfinder.core.network.OverpassClient
import com.example.evfinder.feature.station.StationRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class MapUiState(
    val loading: Boolean = true,
    val stations: List<StationDto> = emptyList(),
    val error: String? = null,
    val pois: List<OverpassClient.Poi> = emptyList(),
    val poiLoading: Boolean = false
)

class MapViewModel : ViewModel() {

    private val repository = StationRepository()
    private val _uiState = MutableStateFlow(MapUiState())
    val uiState: StateFlow<MapUiState> = _uiState
    private var poiJob: Job? = null

    init { load() }

    fun load() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(loading = true, error = null)
            repository.getStations().fold(
                onSuccess = { _uiState.value = _uiState.value.copy(loading = false, stations = it) },
                onFailure = { e -> _uiState.value = _uiState.value.copy(loading = false, error = e.message) }
            )
        }
    }

    fun onMapMoved(south: Double, west: Double, north: Double, east: Double) {
        val maxSpan = 0.35
        val cLat = (south + north) / 2
        val cLng = (west + east) / 2
        val spanLat = minOf(maxSpan, (north - south) / 2)
        val spanLng = minOf(maxSpan, (east - west) / 2)
        poiJob?.cancel()
        poiJob = viewModelScope.launch {
            delay(600)
            _uiState.value = _uiState.value.copy(poiLoading = true)
            val pois = OverpassClient.fuelAndLpg(cLat - spanLat, cLng - spanLng, cLat + spanLat, cLng + spanLng)
            _uiState.value = _uiState.value.copy(poiLoading = false, pois = pois)
        }
    }
}
