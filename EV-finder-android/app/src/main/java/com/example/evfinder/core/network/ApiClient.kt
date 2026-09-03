package com.example.evfinder.core.network

import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Single place where the app talks to the Spring Boot backend.
 * HOST_IP must be the backend PC's LAN IP (ipconfig -> IPv4 Address).
 * Works from a physical phone on the same Wi-Fi and from the emulator.
 */
object ApiClient {

    // TODO: if your PC's IP changes (new Wi-Fi), update HOST_IP.
    private const val HOST_IP = "192.168.0.105"

    const val WS_URL = "ws://$HOST_IP:8080/ws"
    private const val BASE_URL = "http://$HOST_IP:8080"

    val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .addInterceptor(AuthInterceptor()) // JWT on every request
            .build()
    }

    val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
}
