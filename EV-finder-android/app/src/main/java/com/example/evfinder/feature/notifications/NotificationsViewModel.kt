package com.example.evfinder.feature.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.evfinder.core.model.NotificationDto
import com.example.evfinder.core.network.ApiClient
import com.example.evfinder.core.network.ApiService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject
import retrofit2.Response
import java.io.IOException

data class NotificationsUiState(
    val loading: Boolean = true,
    val notifications: List<NotificationDto> = emptyList(),
    val unread: Long = 0,
    val error: String? = null
)

class NotificationsViewModel : ViewModel() {

    private val api: ApiService = ApiClient.retrofit.create(ApiService::class.java)

    private val _uiState = MutableStateFlow(NotificationsUiState())
    val uiState: StateFlow<NotificationsUiState> = _uiState

    init { load() }

    fun load() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(loading = true, error = null)
            apiCall { api.myNotifications() }.fold(
                onSuccess = { list ->
                    _uiState.value = _uiState.value.copy(
                        loading = false, notifications = list,
                        unread = list.count { !it.read }.toLong()
                    )
                },
                onFailure = { e -> _uiState.value = _uiState.value.copy(loading = false, error = e.message) }
            )
        }
    }

    fun markRead(id: String) {
        viewModelScope.launch {
            apiCall { api.markNotificationRead(id) }
            _uiState.value = _uiState.value.copy(
                notifications = _uiState.value.notifications.map {
                    if (it.id == id) it.copy(read = true) else it
                }
            )
            _uiState.value = _uiState.value.copy(
                unread = _uiState.value.notifications.count { !it.read }.toLong()
            )
        }
    }

    fun markAllRead() {
        viewModelScope.launch {
            apiCall { api.markAllNotificationsRead() }
            _uiState.value = _uiState.value.copy(
                notifications = _uiState.value.notifications.map { it.copy(read = true) },
                unread = 0
            )
        }
    }

    private suspend fun <T> apiCall(call: suspend () -> Response<T>): Result<T> {
        return try {
            val response = call()
            val body = response.body()
            if (response.isSuccessful && body != null) Result.success(body)
            else if (response.isSuccessful) Result.success(Unit as T)
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
