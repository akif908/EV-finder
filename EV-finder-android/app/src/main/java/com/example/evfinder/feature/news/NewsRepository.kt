package com.example.evfinder.feature.news

import com.example.evfinder.core.model.NewsDto
import com.example.evfinder.core.model.NewsRequestDto
import com.example.evfinder.core.network.ApiClient
import com.example.evfinder.core.network.ApiService
import org.json.JSONObject
import retrofit2.Response
import java.io.IOException

/**
 * Energy & fuel news — published feed for every user, full CRUD for admins.
 * Same Result<T> pattern as the other feature repositories.
 */
class NewsRepository {

    private val api: ApiService = ApiClient.retrofit.create(ApiService::class.java)

    // ---- Feed (published items; drafts included only for admin listing) ----

    suspend fun getNews(category: String? = null, includeUnpublished: Boolean = false): Result<List<NewsDto>> =
        handle { api.getNews(category?.takeIf { it.isNotBlank() }, includeUnpublished) }

    suspend fun getNewsItem(id: String): Result<NewsDto> =
        handle { api.getNewsItem(id) }

    // ---- Admin management ----

    suspend fun createNews(body: NewsRequestDto): Result<NewsDto> =
        handle { api.createNews(body) }

    suspend fun updateNews(id: String, body: NewsRequestDto): Result<NewsDto> =
        handle { api.updateNews(id, body) }

    suspend fun setPublished(id: String, published: Boolean): Result<NewsDto> =
        handle { api.setNewsPublished(id, published) }

    suspend fun deleteNews(id: String): Result<Unit> =
        handle { api.deleteNews(id) }

    private suspend fun <T> handle(call: suspend () -> Response<T>): Result<T> {
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
            403 -> serverMessage ?: "Only admins can manage news"
            else -> serverMessage ?: "Request failed (HTTP ${response.code()})"
        }
    }
}
