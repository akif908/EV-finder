package com.example.evfinder.feature.support

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.evfinder.core.model.IssueCategories
import com.example.evfinder.core.model.IssueDto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class ReportIssueUiState(
    val myIssues: List<IssueDto> = emptyList(),
    val loading: Boolean = false,
    val submitting: Boolean = false,
    val error: String? = null,
    /** Set after a successful submit so the screen can confirm and close. */
    val submitted: Boolean = false,
    val submittedRef: String? = null
)

/** Backs both the user's "Report an issue" screen and their report history. */
class ReportIssueViewModel : ViewModel() {

    private val repository = IssueRepository()
    private val _uiState = MutableStateFlow(ReportIssueUiState())
    val uiState: StateFlow<ReportIssueUiState> = _uiState

    init {
        loadMine()
    }

    fun loadMine() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(loading = true, error = null)
            repository.mine().fold(
                onSuccess = { _uiState.value = _uiState.value.copy(loading = false, myIssues = it) },
                onFailure = { _uiState.value = _uiState.value.copy(loading = false, error = it.message) }
            )
        }
    }

    fun submit(category: String, subject: String, description: String, stationId: String? = null) {
        if (subject.isBlank() || description.isBlank()) {
            _uiState.value = _uiState.value.copy(error = "Add a subject and a description")
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(submitting = true, error = null)
            repository.create(category.ifBlank { IssueCategories.ALL.last() }, subject, description, stationId)
                .fold(
                    onSuccess = { issue ->
                        _uiState.value = _uiState.value.copy(
                            submitting = false,
                            submitted = true,
                            submittedRef = issue.id.take(8).uppercase(),
                            myIssues = listOf(issue) + _uiState.value.myIssues
                        )
                    },
                    onFailure = { e ->
                        _uiState.value = _uiState.value.copy(submitting = false, error = e.message)
                    }
                )
        }
    }

    /** Clears the success state so the form can be reused. */
    fun acknowledgeSubmit() {
        _uiState.value = _uiState.value.copy(submitted = false, submittedRef = null)
    }
}
