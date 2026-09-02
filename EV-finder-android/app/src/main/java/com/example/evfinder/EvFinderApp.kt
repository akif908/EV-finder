package com.example.evfinder

import android.app.Application
import com.example.evfinder.core.storage.TokenStore

/** App entry point; exposes the shared TokenStore to the network layer. */
class EvFinderApp : Application() {

    val tokenStore: TokenStore by lazy { TokenStore(this) }

    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    companion object {
        lateinit var instance: EvFinderApp
            private set
    }
}
