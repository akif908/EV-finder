package com.example.evfinder.core.network

import com.google.gson.annotations.SerializedName
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST
import java.util.concurrent.TimeUnit

/**
 * Overpass API — free OSM POI service. Falls back to a Dhaka seed when
 * the public endpoint is rate-limited (common).
 */
object OverpassClient {

    data class Poi(val lat: Double, val lng: Double, val name: String, val lpg: Boolean)

    private data class OverpassCenter(val lat: Double? = null, val lon: Double? = null)
    private data class OverpassElement(
        val type: String? = null,
        val lat: Double? = null,
        val lon: Double? = null,
        val center: OverpassCenter? = null,
        val tags: Map<String, String>? = null
    )
    private data class OverpassResponse(@SerializedName("elements") val elements: List<OverpassElement> = emptyList())

    private interface OverpassApi {
        @FormUrlEncoded
        @POST("api/interpreter")
        suspend fun query(@Field("data") query: String): retrofit2.Response<OverpassResponse>
    }

    private val apis: List<OverpassApi> = listOf(
        "https://overpass-api.de/",
        "https://overpass.kumi.systems/"
    ).map { baseUrl ->
        Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(
                OkHttpClient.Builder()
                    .connectTimeout(15, TimeUnit.SECONDS)
                    .readTimeout(20, TimeUnit.SECONDS)
                    .build()
            )
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(OverpassApi::class.java)
    }

    suspend fun fuelAndLpg(south: Double, west: Double, north: Double, east: Double): List<Poi> {
        val q = "[out:json][timeout:25];" +
            "(node[\"amenity\"=\"fuel\"]($south,$west,$north,$east);" +
            "way[\"amenity\"=\"fuel\"]($south,$west,$north,$east););" +
            "out center 120;"
        for (api in apis) {
            val result = runCatching {
                api.query(q).body()?.elements?.mapNotNull { el ->
                    val lat = el.lat ?: el.center?.lat ?: return@mapNotNull null
                    val lng = el.lon ?: el.center?.lon ?: return@mapNotNull null
                    Poi(lat, lng, el.tags?.get("name") ?: "Fuel Station", el.tags?.get("fuel:lpg") == "yes")
                } ?: emptyList()
            }
            result.getOrNull()?.let { if (it.isNotEmpty()) return it }
        }
        return dhakaSeed.filter { it.lat in south..north && it.lng in west..east }
    }

    private val dhakaSeed = listOf(
        Poi(23.7808, 90.4142, "Padma Fuel Station", true),
        Poi(23.7864, 90.4011, "BPC Pump", false),
        Poi(23.7925, 90.4080, "GreenFuel Gulshan", true),
        Poi(23.7662, 90.3890, "Meghna Petroleum", false),
        Poi(23.7771, 90.4150, "Eureka CNG & Fuel", true),
        Poi(23.7510, 90.3840, "Jamuna Oil Pump", false),
        Poi(23.8050, 90.3680, "Mirpur Pump House", true),
        Poi(23.7600, 90.4050, "Farmgate Petro Center", false),
        Poi(23.7320, 90.4120, "Motijheel CNG Station", true),
        Poi(23.7520, 90.4260, "Banasree Fuel Point", false),
        Poi(23.8330, 90.4095, "Khilkhet Fuel Stop", true),
        Poi(23.8500, 90.3980, "Airport Road Petrol", false),
        Poi(23.7900, 90.4250, "Bashundhara Pump", true),
        Poi(23.7250, 90.4080, "Old Dhaka CNG", false),
        Poi(23.7700, 90.3620, "Mohammadpur Pump", true)
    )
}
