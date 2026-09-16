package com.example.evfinder.feature.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.evfinder.EvFinderApp
import com.example.evfinder.feature.booking.BookingRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class ProfileUiState(
    val name: String = "—",
    val email: String = "—",
    val role: String = "USER",
    val bookingCount: Int = 0,
    val vehicleCount: Int = 0,
    val loading: Boolean = true,
    val error: String? = null
)

class ProfileViewModel : ViewModel() {

    private val repository = BookingRepository()

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState

    init { load() }

    fun load() {
        val tokenStore = EvFinderApp.instance.tokenStore
        viewModelScope.launch {
            var bookingCount = 0
            var vehicleCount = 0
            var error: String? = null
            repository.myBookings().fold(
                onSuccess = { bookingCount = it.size },
                onFailure = { error = it.message }
            )
            if (error == null) {
                repository.myVehicles().fold(
                    onSuccess = { vehicleCount = it.size },
                    onFailure = { error = it.message }
                )
            }
            _uiState.value = ProfileUiState(
                name = tokenStore.name ?: "EV Owner",
                email = tokenStore.email ?: "—",
                role = tokenStore.role ?: "USER",
                bookingCount = bookingCount,
                vehicleCount = vehicleCount,
                loading = false,
                error = error
            )
        }
    }

    fun logout(onLoggedOut: () -> Unit) {
        EvFinderApp.instance.tokenStore.clear()
        onLoggedOut()
    }
}
