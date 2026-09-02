package com.example.evfinder.core.storage

import android.content.Context
import android.content.SharedPreferences

/**
 * Minimal JWT + role storage backed by SharedPreferences.
 * Good enough for the course project demo; the token is the app's only
 * "session" — the backend re-validates it on every request.
 */
class TokenStore(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("ev_finder_auth", Context.MODE_PRIVATE)

    fun save(token: String, userId: String, role: String) {
        prefs.edit()
            .putString(KEY_TOKEN, token)
            .putString(KEY_USER_ID, userId)
            .putString(KEY_ROLE, role)
            .apply()
    }

    val token: String? get() = prefs.getString(KEY_TOKEN, null)
    val userId: String? get() = prefs.getString(KEY_USER_ID, null)
    val role: String? get() = prefs.getString(KEY_ROLE, null)

    val isLoggedIn: Boolean get() = token != null

    fun clear() = prefs.edit().clear().apply()

    private companion object {
        const val KEY_TOKEN = "token"
        const val KEY_USER_ID = "userId"
        const val KEY_ROLE = "role"
    }
}
