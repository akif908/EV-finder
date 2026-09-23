package com.example.evfinder.feature.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.evfinder.core.model.AdminOverviewDto
import com.example.evfinder.core.model.AdminUserDto
import com.example.evfinder.core.model.BookingDto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class AdminDashboardUiState(
    val loading: Boolean = true,
    val overview: AdminOverviewDto? = null,
    val recentBookings: List<BookingDto> = emptyList(),
    val error: String? = null
)

class AdminDashboardViewModel : ViewModel() {
    private val repository = AdminRepository()
    private val _uiState = MutableStateFlow(AdminDashboardUiState())
    val uiState: StateFlow<AdminDashboardUiState> = _uiState
    init { load() }
    fun load() {
        viewModelScope.launch {
            var overview: AdminOverviewDto? = null
            var bookings: List<BookingDto> = emptyList()
            var error: String? = null
            repository.overview().fold(onSuccess = { overview = it }, onFailure = { error = it.message })
            if (error == null) repository.bookings().fold(onSuccess = { bookings = it }, onFailure = { error = it.message })
            _uiState.value = AdminDashboardUiState(false, overview, bookings.take(5), error)
        }
    }
}

data class AdminUsersUiState(
    val loading: Boolean = true,
    val users: List<AdminUserDto> = emptyList(),
    val roleFilter: String? = null,
    val error: String? = null,
    val roleDialogUser: AdminUserDto? = null,
    val deleteConfirmUser: AdminUserDto? = null,
    val busyId: String? = null
)

class AdminUsersViewModel : ViewModel() {
    private val repository = AdminRepository()
    private val _uiState = MutableStateFlow(AdminUsersUiState())
    val uiState: StateFlow<AdminUsersUiState> = _uiState
    init { load() }
    fun load() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(loading = true, error = null)
            repository.users(_uiState.value.roleFilter).fold(
                onSuccess = { u -> _uiState.value = _uiState.value.copy(loading = false, users = u) },
                onFailure = { e -> _uiState.value = _uiState.value.copy(loading = false, error = e.message) }
            )
        }
    }
    fun setFilter(role: String?) { _uiState.value = _uiState.value.copy(roleFilter = role); load() }
    fun openRoleDialog(user: AdminUserDto) { _uiState.value = _uiState.value.copy(roleDialogUser = user) }
    fun openDeleteDialog(user: AdminUserDto) { _uiState.value = _uiState.value.copy(deleteConfirmUser = user) }
    fun closeDialogs() { _uiState.value = _uiState.value.copy(roleDialogUser = null, deleteConfirmUser = null) }
    fun changeRole(userId: String, role: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(busyId = userId, error = null)
            repository.changeRole(userId, role).fold(
                onSuccess = { updated ->
                    _uiState.value = _uiState.value.copy(busyId = null, roleDialogUser = null,
                        users = _uiState.value.users.map { if (it.id == updated.id) updated else it })
                },
                onFailure = { e -> _uiState.value = _uiState.value.copy(busyId = null, error = e.message) }
            )
        }
    }
    fun deleteUser(userId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(busyId = userId, error = null)
            repository.deleteUser(userId).fold(
                onSuccess = {
                    _uiState.value = _uiState.value.copy(busyId = null, deleteConfirmUser = null,
                        users = _uiState.value.users.filter { it.id != userId })
                },
                onFailure = { e -> _uiState.value = _uiState.value.copy(busyId = null, deleteConfirmUser = null, error = e.message) }
            )
        }
    }
}

data class AdminBookingsUiState(
    val loading: Boolean = true,
    val bookings: List<BookingDto> = emptyList(),
    val error: String? = null,
    val cancellingId: String? = null
)

class AdminBookingsViewModel : ViewModel() {
    private val repository = AdminRepository()
    private val _uiState = MutableStateFlow(AdminBookingsUiState())
    val uiState: StateFlow<AdminBookingsUiState> = _uiState
    init { load() }
    fun load() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(loading = true, error = null)
            repository.bookings().fold(
                onSuccess = { b -> _uiState.value = _uiState.value.copy(loading = false, bookings = b) },
                onFailure = { e -> _uiState.value = _uiState.value.copy(loading = false, error = e.message) }
            )
        }
    }
    fun forceCancel(id: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(cancellingId = id, error = null)
            repository.forceCancelBooking(id).fold(
                onSuccess = { updated ->
                    _uiState.value = _uiState.value.copy(cancellingId = null,
                        bookings = _uiState.value.bookings.map { if (it.id == updated.id) updated else it })
                },
                onFailure = { e -> _uiState.value = _uiState.value.copy(cancellingId = null, error = e.message) }
            )
        }
    }
}

// ─── Issue reports inbox ─────────────────────────────────────────────────────
data class AdminIssuesUiState(
    val loading: Boolean = true,
    val issues: List<com.example.evfinder.core.model.IssueDto> = emptyList(),
    val filter: String = "OPEN",
    val error: String? = null,
    val updatingId: String? = null
)

class AdminIssuesViewModel : ViewModel() {
    private val repository = AdminRepository()
    private val _uiState = MutableStateFlow(AdminIssuesUiState())
    val uiState: StateFlow<AdminIssuesUiState> = _uiState

    init { load() }

    fun load() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(loading = true, error = null)
            repository.issues().fold(
                onSuccess = { list -> _uiState.value = _uiState.value.copy(loading = false, issues = list) },
                onFailure = { e -> _uiState.value = _uiState.value.copy(loading = false, error = e.message) }
            )
        }
    }

    fun setFilter(filter: String) { _uiState.value = _uiState.value.copy(filter = filter) }

    /** Moves a report along and, optionally, sends the reporter a reply. */
    fun update(id: String, status: String, note: String?) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(updatingId = id, error = null)
            repository.updateIssue(id, status, note).fold(
                onSuccess = { updated ->
                    _uiState.value = _uiState.value.copy(
                        updatingId = null,
                        issues = _uiState.value.issues.map { if (it.id == updated.id) updated else it }
                    )
                },
                onFailure = { e -> _uiState.value = _uiState.value.copy(updatingId = null, error = e.message) }
            )
        }
    }
}
