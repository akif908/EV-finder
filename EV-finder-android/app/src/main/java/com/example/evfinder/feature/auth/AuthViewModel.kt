package com.example.evfinder.feature.auth

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.evfinder.core.storage.TokenStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/** UI state for both login and register screens. */
data class AuthUiState(
    val loading: Boolean = false,
    val error: String? = null,
    val success: Boolean = false
)

class AuthViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = AuthRepository()
    private val tokenStore = TokenStore(application)

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState

    fun login(email: String, password: String) {
        submit { repository.login(email, password) }
    }

    fun register(name: String, email: String, password: String, role: String) {
        submit { repository.register(name, email, password, role) }
    }

    /** On success the JWT + role are persisted; the app navigates and every
     *  later request will attach the token (backend re-validates each time). */
    private fun submit(call: suspend () -> Result<com.example.evfinder.core.model.AuthResponse>) {
        _uiState.value = AuthUiState(loading = true)
        viewModelScope.launch {
            call().fold(
                onSuccess = { auth ->
                    tokenStore.save(auth.token, auth.userId, auth.role, auth.name, auth.email)
                    _uiState.value = AuthUiState(success = true)
                },
                onFailure = { e ->
                    _uiState.value = AuthUiState(error = e.message)
                }
            )
        }
    }
}
