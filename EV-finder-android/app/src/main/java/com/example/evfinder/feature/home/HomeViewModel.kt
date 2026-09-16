package com.example.evfinder.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.evfinder.EvFinderApp
import com.example.evfinder.core.model.BookingDto
import com.example.evfinder.core.model.StationDto
import com.example.evfinder.core.network.AvailabilitySocket
import com.example.evfinder.core.network.OverpassClient
import com.example.evfinder.feature.booking.BookingRepository
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
    val error: String? = null,
    // dashboard extras
    val upcoming: BookingDto? = null,
    val totalBookings: Int = 0,
    val liveUpdates: Boolean = false,
    val liveTick: Int = 0,
    // real-world fuel/LPG POIs (OpenStreetMap) for the map preview
    val pois: List<OverpassClient.Poi> = emptyList()
)

class HomeViewModel : ViewModel() {

    private val stationRepository = StationRepository()
    private val bookingRepository = BookingRepository()

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState

    private val queryFlow = MutableStateFlow("")

    init {
        loadDashboard()
        loadStations()
        loadPois()

        // debounced search
        viewModelScope.launch {
            @OptIn(FlowPreview::class)
            queryFlow.debounce(400).distinctUntilChanged().collect { loadStations(it) }
        }

        // live availability: any broadcast triggers a silent refresh + tick
        viewModelScope.launch {
            AvailabilitySocket.events.collect {
                _uiState.value = _uiState.value.copy(liveTick = _uiState.value.liveTick + 1)
                loadStations(_uiState.value.query, silent = true)
                loadDashboard(silent = true)
            }
        }
        AvailabilitySocket.connect()
        _uiState.value = _uiState.value.copy(liveUpdates = AvailabilitySocket.connected)
    }

    fun onQueryChanged(query: String) {
        _uiState.value = _uiState.value.copy(query = query)
        queryFlow.value = query
    }

    fun refresh() {
        loadStations(_uiState.value.query)
        loadDashboard()
    }

    /** Background refresh without the loading spinner — used when re-entering the tab. */
    fun silentRefresh() {
        loadStations(_uiState.value.query, silent = true)
        loadDashboard(silent = true)
    }

    private fun loadDashboard(silent: Boolean = false) {
        viewModelScope.launch {
            if (!silent) _uiState.value = _uiState.value.copy(loading = true)
            bookingRepository.myBookings().fold(
                onSuccess = { bookings ->
                    val upcoming = bookings
                        .filter { (it.status == "CONFIRMED" || it.status == "PENDING") }
                        .minByOrNull { it.startTime }
                    _uiState.value = _uiState.value.copy(
                        loading = false,
                        upcoming = upcoming,
                        totalBookings = bookings.size,
                        liveUpdates = AvailabilitySocket.connected
                    )
                },
                onFailure = { e ->
                    if (!silent) _uiState.value = _uiState.value.copy(loading = false, error = e.message)
                }
            )
        }
    }

    /** One-time fetch of fuel/LPG stations around Dhaka for the map preview. */
    private fun loadPois() {
        viewModelScope.launch {
            // fixed metro bbox (south, west, north, east)
            val pois = OverpassClient.fuelAndLpg(23.68, 90.32, 23.90, 90.48)
            _uiState.value = _uiState.value.copy(pois = pois)
        }
    }

    private fun loadStations(query: String? = null, silent: Boolean = false) {
        viewModelScope.launch {
            if (!silent) _uiState.value = _uiState.value.copy(loading = true, error = null)
            stationRepository.getStations(query).fold(
                onSuccess = { stations ->
                    _uiState.value = _uiState.value.copy(loading = false, stations = stations)
                },
                onFailure = { e ->
                    if (!silent) _uiState.value = _uiState.value.copy(loading = false, error = e.message)
                }
            )
        }
    }
}
