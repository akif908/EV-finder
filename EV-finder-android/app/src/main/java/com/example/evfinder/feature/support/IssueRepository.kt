package com.example.evfinder.feature.support

import com.example.evfinder.core.model.IssueDto
import com.example.evfinder.core.model.IssueRequestDto
import com.example.evfinder.core.model.IssueUpdateDto
import com.example.evfinder.core.network.ApiClient
import com.example.evfinder.core.network.ApiService
import retrofit2.Response

/** Thin wrapper over the issue-report endpoints. */
class IssueRepository(private val api: ApiService = ApiClient.retrofit.create(ApiService::class.java)) {

    suspend fun create(category: String, subject: String, description: String, stationId: String?):
        Result<IssueDto> = handle { api.createIssue(IssueRequestDto(category, subject, description, stationId)) }

    suspend fun mine(): Result<List<IssueDto>> = handle { api.myIssues() }

    suspend fun all(): Result<List<IssueDto>> = handle { api.adminIssues() }

    suspend fun update(id: String, status: String, note: String?): Result<IssueDto> =
        handle { api.adminUpdateIssue(id, IssueUpdateDto(status, note)) }

    private suspend fun <T> handle(call: suspend () -> Response<T>): Result<T> {
        return try {
            val response = call()
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null) Result.success(body) else Result.failure(IllegalStateException("Empty response"))
            } else {
                Result.failure(IllegalStateException(errorMessage(response)))
            }
        } catch (e: Exception) {
            Result.failure(IllegalStateException(e.message ?: "Network error"))
        }
    }

    private fun <T> errorMessage(response: Response<T>): String = when (response.code()) {
        401 -> "Session expired — please log in again"
        403 -> "You don't have access to this"
        else -> "Request failed (${response.code()})"
    }
}
