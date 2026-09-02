package com.example.evfinder.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.evfinder.core.model.StationDto
import com.example.evfinder.feature.station.StationRepository
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

data class HomeUiState(
    val loading: Boolean = false,
    val stations: List<StationDto> = emptyList(),
    val query: String = "",
    val error: String? = null
)

class HomeViewModel : ViewModel() {

    private val repository = StationRepository()

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState

    private val queryFlow = MutableStateFlow("")

    init {
        loadStations()
        // debounce search: one request after typing pauses 400ms
        viewModelScope.launch {
            @OptIn(FlowPreview::class)
            queryFlow.debounce(400).distinctUntilChanged().collect { q ->
                loadStations(q)
            }
        }
    }

    fun onQueryChanged(query: String) {
        _uiState.value = _uiState.value.copy(query = query)
        queryFlow.value = query
    }

    fun refresh() = loadStations(_uiState.value.query)

    private fun loadStations(query: String? = null) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(loading = true, error = null)
            repository.getStations(query).fold(
                onSuccess = { stations ->
                    _uiState.value = _uiState.value.copy(loading = false, stations = stations)
                },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(loading = false, error = e.message)
                }
            )
        }
    }
}
