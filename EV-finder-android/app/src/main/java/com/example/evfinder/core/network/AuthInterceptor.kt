package com.example.evfinder.core.network

import com.example.evfinder.EvFinderApp
import okhttp3.Interceptor
import okhttp3.Response

/** Attaches "Authorization: Bearer <jwt>" to every request. */
class AuthInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val token = EvFinderApp.instance.tokenStore.token
        val request = if (token != null) {
            chain.request().newBuilder()
                .addHeader("Authorization", "Bearer $token")
                .build()
        } else chain.request()
        return chain.proceed(request)
    }
}
