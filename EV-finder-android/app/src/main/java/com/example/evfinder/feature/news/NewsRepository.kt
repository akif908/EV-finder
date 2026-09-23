package com.example.evfinder.feature.news

import com.example.evfinder.core.model.NewsArticleDto
import com.example.evfinder.core.network.ApiClient
import com.example.evfinder.core.network.ApiService
import retrofit2.Response

/** Reads the cached energy / fuel / EV headlines from the backend. */
class NewsRepository(private val api: ApiService = ApiClient.retrofit.create(ApiService::class.java)) {

    suspend fun latest(category: String? = null, limit: Int = 30): Result<List<NewsArticleDto>> = try {
        val response = api.news(category, limit)
        val body = response.body()
        if (response.isSuccessful && body != null) Result.success(body)
        else Result.failure(IllegalStateException(errorMessage(response)))
    } catch (e: Exception) {
        Result.failure(IllegalStateException(e.message ?: "Network error"))
    }

    private fun <T> errorMessage(response: Response<T>): String = when (response.code()) {
        401 -> "Session expired — please log in again"
        else -> "Could not load updates (${response.code()})"
    }
}
