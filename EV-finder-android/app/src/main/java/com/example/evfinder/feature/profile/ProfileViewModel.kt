package com.example.evfinder.feature.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.evfinder.EvFinderApp
import com.example.evfinder.core.model.ChangePasswordRequest
import com.example.evfinder.core.model.UpdateProfileRequest
import com.example.evfinder.core.model.UserMeDto
import com.example.evfinder.core.network.ApiClient
import com.example.evfinder.core.network.ApiService
import com.example.evfinder.feature.booking.BookingRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject
import retrofit2.Response
import java.io.IOException

data class ProfileUiState(
    val loading: Boolean = true,
    val me: UserMeDto? = null,
    val bookingCount: Int = 0,
    val vehicleCount: Int = 0,
    val error: String? = null,
    // dialogs
    val showEdit: Boolean = false,
    val showPassword: Boolean = false,
    val saving: Boolean = false,
    val toast: String? = null   // transient success message
)

/** Profile self-service — shared by USER, OPERATOR and ADMIN shells. */
class ProfileViewModel : ViewModel() {

    private val api: ApiService = ApiClient.retrofit.create(ApiService::class.java)
    private val bookingRepository = BookingRepository()

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState

    init { load() }

    fun load() {
        viewModelScope.launch {
            var me: UserMeDto? = null
            var bookings = 0
            var vehicles = 0
            var error: String? = null
            apiCall { api.getMe() }.fold(
                onSuccess = { me = it },
                onFailure = { error = it.message }
            )
            // stats are user-flavoured; ignore failures for operator/admin
            bookingRepository.myBookings().onSuccess { bookings = it.size }
            bookingRepository.myVehicles().onSuccess { vehicles = it.size }

            // keep the locally cached name fresh (used in greetings)
            me?.let {
                EvFinderApp.instance.tokenStore.save(
                    EvFinderApp.instance.tokenStore.token ?: "",
                    it.id, it.role, it.name, it.email
                )
            }
            _uiState.value = _uiState.value.copy(
                loading = false, me = me, bookingCount = bookings,
                vehicleCount = vehicles, error = error
            )
        }
    }

    fun openEdit() { _uiState.value = _uiState.value.copy(showEdit = true) }
    fun openPassword() { _uiState.value = _uiState.value.copy(showPassword = true) }
    fun closeDialogs() { _uiState.value = _uiState.value.copy(showEdit = false, showPassword = false, error = null, toast = null) }

    fun saveProfile(name: String, phone: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(saving = true, error = null)
            apiCall { api.updateMe(UpdateProfileRequest(name.trim(), phone.trim().takeIf { it.isNotBlank() })) }.fold(
                onSuccess = { updated ->
                    _uiState.value = _uiState.value.copy(
                        saving = false, showEdit = false, me = updated,
                        toast = "Profile updated"
                    )
                },
                onFailure = { e -> _uiState.value = _uiState.value.copy(saving = false, error = e.message) }
            )
        }
    }

    fun changePassword(oldPassword: String, newPassword: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(saving = true, error = null)
            apiCall { api.changePassword(ChangePasswordRequest(oldPassword, newPassword)) }.fold(
                onSuccess = {
                    _uiState.value = _uiState.value.copy(
                        saving = false, showPassword = false,
                        toast = "Password changed"
                    )
                },
                onFailure = { e -> _uiState.value = _uiState.value.copy(saving = false, error = e.message) }
            )
        }
    }

    fun logout(onLoggedOut: () -> Unit) {
        EvFinderApp.instance.tokenStore.clear()
        onLoggedOut()
    }

    private suspend fun <T> apiCall(call: suspend () -> Response<T>): Result<T> {
        return try {
            val response = call()
            val body = response.body()
            if (response.isSuccessful && body != null) Result.success(body)
            else Result.failure(Exception(errorMessage(response)))
        } catch (e: IOException) {
            Result.failure(Exception("Cannot reach the server. Is the backend running?"))
        }
    }

    private fun <T> errorMessage(response: Response<T>): String {
        val serverMessage = try {
            JSONObject(response.errorBody()?.string() ?: "{}")
                .optString("message", "").takeIf { it.isNotBlank() }
        } catch (e: Exception) { null }
        return when (response.code()) {
            401 -> serverMessage ?: "Session expired — please log in again"
            else -> serverMessage ?: "Request failed (HTTP ${response.code()})"
        }
    }
}
