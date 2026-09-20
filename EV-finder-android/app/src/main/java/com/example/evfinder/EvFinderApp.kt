package com.example.evfinder

import android.app.Application
import com.example.evfinder.core.storage.TokenStore
import java.io.File

/** App entry point; exposes the shared TokenStore to the network layer. */
class EvFinderApp : Application() {

    val tokenStore: TokenStore by lazy { TokenStore(this) }

    override fun onCreate() {
        super.onCreate()
        instance = this

        // osmdroid: OpenStreetMap blocks apps that don't identify themselves
        // (403 "Access blocked" tiles), so send a policy-compliant user agent
        // and keep the tile cache private to this app.
        org.osmdroid.config.Configuration.getInstance().apply {
            userAgentValue = "EV-Finder-Android/1.0 (university course project; contact: $packageName)"
            osmdroidBasePath = File(cacheDir, "osmdroid")
            osmdroidTileCache = File(cacheDir, "osmdroid/tiles")
        }
    }

    companion object {
        lateinit var instance: EvFinderApp
            private set
    }
}
