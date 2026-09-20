package com.example.evfinder.core.storage

import android.content.Context
import android.content.SharedPreferences

/**
 * JWT + role storage backed by SharedPreferences.
 * The token is the app's only session — the backend re-validates it on every request.
 */
class TokenStore(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("ev_finder_auth", Context.MODE_PRIVATE)

    fun save(token: String, userId: String, role: String, name: String = "", email: String = "") {
        prefs.edit()
            .putString(KEY_TOKEN, token)
            .putString(KEY_USER_ID, userId)
            .putString(KEY_ROLE, role)
            .putString(KEY_NAME, name)
            .putString(KEY_EMAIL, email)
            .apply()
    }

    val token: String? get() = prefs.getString(KEY_TOKEN, null)
    val userId: String? get() = prefs.getString(KEY_USER_ID, null)
    val role: String? get() = prefs.getString(KEY_ROLE, null)
    val name: String? get() = prefs.getString(KEY_NAME, null)
    val email: String? get() = prefs.getString(KEY_EMAIL, null)

    val isLoggedIn: Boolean get() = token != null

    fun clear() = prefs.edit().clear().apply()

    private companion object {
        const val KEY_TOKEN = "token"
        const val KEY_USER_ID = "userId"
        const val KEY_ROLE = "role"
        const val KEY_NAME = "name"
        const val KEY_EMAIL = "email"
    }
}
