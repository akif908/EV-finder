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

    data class Poi(
        val lat: Double,
        val lng: Double,
        val name: String,
        val lpg: Boolean,
        /** OSM `brand` / `operator` tag, when the mapper recorded one. */
        val brand: String? = null,
        val address: String? = null,
        /** Raw OSM `opening_hours` string, e.g. `24/7` or `Mo-Su 06:00-22:00`. */
        val openingHours: String? = null,
        /** Fuels the pump sells, derived from the OSM `fuel:*` tags. */
        val fuelTypes: List<String> = emptyList()
    )

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
                    val tags = el.tags.orEmpty()
                    Poi(
                        lat = lat,
                        lng = lng,
                        name = tags["name"] ?: tags["brand"] ?: "Fuel Station",
                        lpg = tags["fuel:lpg"] == "yes",
                        brand = tags["brand"] ?: tags["operator"],
                        address = addressOf(tags),
                        openingHours = tags["opening_hours"],
                        fuelTypes = fuelTypesOf(tags)
                    )
                } ?: emptyList()
            }
            result.getOrNull()?.let { if (it.isNotEmpty()) return it }
        }
        return seedPois(south, west, north, east)
    }

    /** Bangla petrol pumps are tagged with a mix of `fuel:*` keys. */
    private fun fuelTypesOf(tags: Map<String, String>): List<String> = listOf(
        "Petrol" to listOf("fuel:petrol", "fuel:octane_95", "fuel:octane_98"),
        "Diesel" to listOf("fuel:diesel"),
        "CNG" to listOf("fuel:cng"),
        "LPG" to listOf("fuel:lpg")
    ).filter { (_, keys) -> keys.any { tags[it] == "yes" } }.map { it.first }

    private fun addressOf(tags: Map<String, String>): String? {
        tags["addr:full"]?.let { return it }
        val street = listOfNotNull(tags["addr:housenumber"], tags["addr:street"])
            .joinToString(" ")
            .trim()
        return street.ifBlank { tags["addr:suburb"] ?: tags["addr:city"] }
    }

    /**
     * The curated Dhaka pumps inside a bounding box. Callers can paint these
     * immediately instead of waiting on Overpass, which is regularly slow or
     * rate-limited.
     */
    fun seedPois(south: Double, west: Double, north: Double, east: Double): List<Poi> =
        dhakaSeed.filter { it.lat in south..north && it.lng in west..east }

    private val dhakaSeed = listOf(
        Poi(23.7808, 90.4142, "Padma Fuel Station", true, "Padma Oil", "Mirpur Road, Dhanmondi", "24/7", listOf("Petrol", "Diesel", "LPG")),
        Poi(23.7864, 90.4011, "BPC Pump", false, "Bangladesh Petroleum", "Green Road, Kalabagan", "06:00-23:00", listOf("Petrol", "Diesel", "CNG")),
        Poi(23.7925, 90.4080, "GreenFuel Gulshan", true, "GreenFuel BD", "Gulshan Avenue", "24/7", listOf("Petrol", "Diesel", "LPG")),
        Poi(23.7662, 90.3890, "Meghna Petroleum", false, "Meghna Petroleum", "Elephant Road", "06:00-22:00", listOf("Petrol", "Diesel")),
        Poi(23.7771, 90.4150, "Eureka CNG & Fuel", true, "Eureka", "Banglamotor", "24/7", listOf("CNG", "LPG", "Petrol")),
        Poi(23.7510, 90.3840, "Jamuna Oil Pump", false, "Jamuna Oil", "Hazaribagh", "06:00-22:00", listOf("Petrol", "Diesel")),
        Poi(23.8050, 90.3680, "Mirpur Pump House", true, "Padma Oil", "Mirpur 10", "24/7", listOf("Petrol", "Diesel", "CNG", "LPG")),
        Poi(23.7600, 90.4050, "Farmgate Petro Center", false, "Meghna Petroleum", "Farmgate", "06:00-23:00", listOf("Petrol", "Diesel", "CNG")),
        Poi(23.7320, 90.4120, "Motijheel CNG Station", true, "Eureka", "Motijheel C/A", "05:00-23:00", listOf("CNG", "LPG")),
        Poi(23.7520, 90.4260, "Banasree Fuel Point", false, "GreenFuel BD", "Banasree Block C", "06:00-22:00", listOf("Petrol", "Diesel")),
        Poi(23.8330, 90.4095, "Khilkhet Fuel Stop", true, "Padma Oil", "Khilkhet, Airport Road", "24/7", listOf("Petrol", "Diesel", "LPG")),
        Poi(23.8500, 90.3980, "Airport Road Petrol", false, "Bangladesh Petroleum", "Airport Road", "24/7", listOf("Petrol", "Diesel", "CNG")),
        Poi(23.7900, 90.4250, "Bashundhara Pump", true, "Jamuna Oil", "Bashundhara R/A", "06:00-22:00", listOf("Petrol", "Diesel", "LPG")),
        Poi(23.7250, 90.4080, "Old Dhaka CNG", false, "Eureka", "Nayabazar", "05:00-22:00", listOf("CNG")),
        Poi(23.7700, 90.3620, "Mohammadpur Pump", true, "Meghna Petroleum", "Mohammadpur", "24/7", listOf("Petrol", "Diesel", "LPG"))
    )
}
