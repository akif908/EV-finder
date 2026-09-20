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
    // real-world fuel / LPG POIs (OpenStreetMap) for the map preview
    val pois: List<OverpassClient.Poi> = emptyList(),
    /** Search filters/sorting: See [HomeViewModel.SORTS]. */
    val sortMode: String = "RATING",
    val onlyAvailable: Boolean = false,
    /** Stations after applying the active filters + sort. */
    val visibleStations: List<StationDto> = emptyList()
)

class HomeViewModel : ViewModel() {

    private val stationRepository = StationRepository()
    private val bookingRepository = BookingRepository()

    private val _uiState = MutableStateFlow(HomeUiState())

    companion object {
        val SORTS = listOf(
            "RATING" to "Top rated",
            "REVIEWS" to "Most reviewed",
            "PRICE" to "Price",
            "NAME" to "A–Z"
        )
    }
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

    /** Home search filters. */
    fun setSort(mode: String) {
        _uiState.value = _uiState.value.copy(sortMode = mode)
        applyFilters()
    }

    fun toggleOnlyAvailable() {
        _uiState.value = _uiState.value.copy(onlyAvailable = !_uiState.value.onlyAvailable)
        applyFilters()
    }

    /**
     * Applies the home filters client-side: "available only" plus the chosen
     * sort. The backend already returns stations ranked by rating, so RATING
     * mode simply preserves that order.
     */
    private fun applyFilters() {
        val s = _uiState.value
        var list = s.stations
        if (s.onlyAvailable) {
            list = list.filter { st -> st.services.any { it.availableSlots > 0 } }
        }
        list = when (s.sortMode) {
            "REVIEWS" -> list.sortedWith(
                compareByDescending<StationDto> { it.reviewCount }.thenByDescending { it.averageRating }
            )
            "PRICE" -> list.sortedBy { st -> st.services.minOfOrNull { it.pricePerUnit } ?: Double.MAX_VALUE }
            "NAME" -> list.sortedBy { it.name.lowercase() }
            else -> list.sortedWith(
                compareByDescending<StationDto> { it.averageRating }.thenByDescending { it.reviewCount }
            )
        }
        _uiState.value = s.copy(visibleStations = list)
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

    /** Fuel/LPG stations around Dhaka for the map preview layer. */
    private fun loadPois() {
        viewModelScope.launch {
            val pois = OverpassClient.fuelAndLpg(23.68, 90.32, 23.90, 90.48)
            _uiState.value = _uiState.value.copy(pois = pois)
        }
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

    private fun loadStations(query: String? = null, silent: Boolean = false) {
        viewModelScope.launch {
            if (!silent) _uiState.value = _uiState.value.copy(loading = true, error = null)
            stationRepository.getStations(query).fold(
                onSuccess = { stations ->
                    _uiState.value = _uiState.value.copy(loading = false, stations = stations)
                    applyFilters()
                },
                onFailure = { e ->
                    if (!silent) _uiState.value = _uiState.value.copy(loading = false, error = e.message)
                }
            )
        }
    }
}
