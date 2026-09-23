package com.example.evfinder.feature.news

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.evfinder.core.model.NewsArticleDto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class NewsUiState(
    val loading: Boolean = true,
    val articles: List<NewsArticleDto> = emptyList(),
    val category: String? = null,
    val error: String? = null
)

/**
 * Energy / fuel / EV headlines, fetched from the backend cache.
 * `previewLimit` > 0 keeps the list short for the Home carousel.
 */
class NewsViewModel(private val previewLimit: Int = 0) : ViewModel() {

    private val repository = NewsRepository()
    private val _uiState = MutableStateFlow(NewsUiState())
    val uiState: StateFlow<NewsUiState> = _uiState

    init { load() }

    fun setCategory(category: String?) {
        _uiState.value = _uiState.value.copy(category = category)
        load()
    }

    fun load() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(loading = true, error = null)
            repository.latest(_uiState.value.category, if (previewLimit > 0) previewLimit else 40).fold(
                onSuccess = { list ->
                    _uiState.value = _uiState.value.copy(loading = false, articles = list)
                },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(loading = false, error = e.message)
                }
            )
        }
    }
}
