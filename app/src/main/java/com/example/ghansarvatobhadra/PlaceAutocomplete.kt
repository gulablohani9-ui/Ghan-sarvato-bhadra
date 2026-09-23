package com.example.ghansarvatobhadra

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

data class PlaceResult(
    val displayName: String,
    val shortName: String,
    val latitude: Double,
    val longitude: Double
)

object PlaceAutocomplete {

    suspend fun search(query: String): List<PlaceResult> = withContext(Dispatchers.IO) {
        if (query.length < 3) return@withContext emptyList()
        runCatching {
            val url = "https://nominatim.openstreetmap.org/search?" +
                    "q=${URLEncoder.encode(query, "UTF-8")}" +
                    "&format=json&limit=5&addressdetails=1"
            val conn = (URL(url).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                setRequestProperty("User-Agent", "GhanSarvatoBhadra/1.0")
                connectTimeout = 10000
                readTimeout = 10000
            }
            val text = conn.inputStream.bufferedReader().use { it.readText() }
            val arr = JSONArray(text)
            buildList {
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    val full = o.getString("display_name")
                    add(PlaceResult(
                        displayName = full,
                        shortName = full.split(",").first().trim(),
                        latitude = o.getString("lat").toDouble(),
                        longitude = o.getString("lon").toDouble()
                    ))
                }
            }
        }.getOrDefault(emptyList())
    }
}
