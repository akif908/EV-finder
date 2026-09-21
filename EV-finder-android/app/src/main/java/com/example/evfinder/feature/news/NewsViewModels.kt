package com.example.evfinder.feature.news

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.evfinder.core.model.NewsDto
import com.example.evfinder.core.model.NewsRequestDto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * View All News: published feed with server-side category filter and
 * client-side search (small dataset — instant filtering, no extra endpoint).
 */
data class NewsListUiState(
    val loading: Boolean = false,
    val news: List<NewsDto> = emptyList(),
    val category: String? = null,      // null = All categories
    val query: String = "",
    val error: String? = null
) {
    /** Search narrows the already-loaded list by title/description/source. */
    val visibleNews: List<NewsDto>
        get() = if (query.isBlank()) news
        else news.filter {
            it.title.contains(query, ignoreCase = true) ||
            it.shortDescription.contains(query, ignoreCase = true) ||
            (it.source ?: "").contains(query, ignoreCase = true)
        }
}

class NewsListViewModel : ViewModel() {

    private val repository = NewsRepository()

    private val _uiState = MutableStateFlow(NewsListUiState())
    val uiState: StateFlow<NewsListUiState> = _uiState

    init { load() }

    fun load() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(loading = true, error = null)
            repository.getNews(category = _uiState.value.category).fold(
                onSuccess = { news ->
                    _uiState.value = _uiState.value.copy(loading = false, news = news)
                },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(loading = false, error = e.message)
                }
            )
        }
    }

    fun setCategory(category: String?) {
        if (_uiState.value.category == category) return
        _uiState.value = _uiState.value.copy(category = category)
        load()
    }

    fun onQueryChanged(query: String) {
        _uiState.value = _uiState.value.copy(query = query)
    }
}

/** News detail: single published item (drafts resolve only for admins). */
data class NewsDetailUiState(
    val loading: Boolean = false,
    val news: NewsDto? = null,
    val error: String? = null
)

class NewsDetailViewModel(newsId: String) : ViewModel() {

    private val repository = NewsRepository()

    private val _uiState = MutableStateFlow(NewsDetailUiState(loading = true))
    val uiState: StateFlow<NewsDetailUiState> = _uiState

    init {
        viewModelScope.launch {
            repository.getNewsItem(newsId).fold(
                onSuccess = { news -> _uiState.value = NewsDetailUiState(news = news) },
                onFailure = { e -> _uiState.value = NewsDetailUiState(error = e.message) }
            )
        }
    }
}

/** Admin news management: drafts included, full CRUD + publish toggle. */
data class AdminNewsUiState(
    val loading: Boolean = false,
    val news: List<NewsDto> = emptyList(),
    val saving: Boolean = false,
    val error: String? = null
)

class AdminNewsViewModel : ViewModel() {

    private val repository = NewsRepository()

    private val _uiState = MutableStateFlow(AdminNewsUiState())
    val uiState: StateFlow<AdminNewsUiState> = _uiState

    init { load() }

    fun load() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(loading = true, error = null)
            repository.getNews(includeUnpublished = true).fold(
                onSuccess = { news ->
                    _uiState.value = _uiState.value.copy(loading = false, news = news)
                },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(loading = false, error = e.message)
                }
            )
        }
    }

    fun save(
        editingId: String?,
        title: String, shortDescription: String, content: String,
        category: String, source: String, sourceUrl: String,
        imageUrl: String, publishedAt: String, isPublished: Boolean,
        onDone: (String?) -> Unit   // null = success, else error message
    ) {
        val body = NewsRequestDto(
            title = title.trim(),
            shortDescription = shortDescription.trim(),
            content = content.trim(),
            category = category,
            source = source.trim().ifBlank { null },
            sourceUrl = sourceUrl.trim().ifBlank { null },
            imageUrl = imageUrl.trim().ifBlank { null },
            publishedAt = publishedAt.trim(),
            isPublished = isPublished
        )
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(saving = true)
            val result = if (editingId == null) repository.createNews(body)
                         else repository.updateNews(editingId, body)
            result.fold(
                onSuccess = {
                    _uiState.value = _uiState.value.copy(saving = false)
                    load()
                    onDone(null)
                },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(saving = false)
                    onDone(e.message)
                }
            )
        }
    }

    fun setPublished(newsId: String, published: Boolean) {
        viewModelScope.launch {
            repository.setPublished(newsId, published).fold(
                onSuccess = { load() },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(error = e.message)
                    load() // revert the switch to the server truth
                }
            )
        }
    }

    fun delete(newsId: String) {
        viewModelScope.launch {
            repository.deleteNews(newsId).fold(
                onSuccess = { load() },
                onFailure = { e -> _uiState.value = _uiState.value.copy(error = e.message) }
            )
        }
    }
}
