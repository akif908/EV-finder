package com.example.evfinder.feature.notifications

import com.example.evfinder.core.network.ApiClient
import com.example.evfinder.core.network.ApiService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * App-wide unread-notification counter.
 *
 * The bell in each app bar observes this, so a new notification (a booking, a
 * payment failure, an admin's reply to a report) lights the badge up no matter
 * which screen the user is on. Polled rather than pushed because notifications
 * are created server-side by many flows and the app only opens the WebSocket
 * for availability events.
 */
object UnreadNotifications {

    private val api: ApiService = ApiClient.retrofit.create(ApiService::class.java)

    private val _count = MutableStateFlow(0)
    val count: StateFlow<Int> = _count

    private var poller: Job? = null

    /** Starts the background poll once; safe to call from every app shell. */
    fun start(scope: CoroutineScope, intervalMs: Long = 20_000) {
        if (poller?.isActive == true) return
        poller = scope.launch {
            while (isActive) {
                refresh()
                delay(intervalMs)
            }
        }
    }

    /** Re-reads the unread count now (after opening the inbox, or logging out). */
    suspend fun refresh() {
        try {
            val response = api.unreadCount()
            if (response.isSuccessful) {
                response.body()?.let { _count.value = it.count.toInt() }
            }
        } catch (_: Exception) {
            // transient network/auth issue — keep the last known count
        }
    }

    /** Clears the badge immediately, e.g. after "mark all read". */
    fun setLocal(count: Int) {
        _count.value = count.coerceAtLeast(0)
    }

    fun stop() {
        poller?.cancel()
        poller = null
        _count.value = 0
    }
}
