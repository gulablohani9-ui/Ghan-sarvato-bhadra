package com.example.ghansarvatobhadra

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

data class ChartResponse(
    val bodies: List<PlanetPosition>,
    val tithi: Int,
    val weekday: Int,
    val lagna: PlanetPosition?
)

class EphemerisApi(private val base: String) {

    suspend fun chart(
        datetime: String,
        tz: String,
        lat: Double,
        lon: Double
    ): Result<ChartResponse> = withContext(Dispatchers.IO) {
        runCatching {
            val q = "v1/chart?datetime=${URLEncoder.encode(datetime, "UTF-8")}" +
                    "&tz=${URLEncoder.encode(tz, "UTF-8")}" +
                    "&lat=$lat&lon=$lon&ayanamsha=lahiri&nodes=mean"
            val c = (URL(base.trimEnd('/') + "/" + q).openConnection() as HttpURLConnection).apply {
                connectTimeout = 30000
                readTimeout = 45000
                requestMethod = "GET"
            }
            if (c.responseCode !in 200..299) error("HTTP ${c.responseCode}")
            val o = JSONObject(c.inputStream.bufferedReader().use { it.readText() })
            val arr = o.getJSONArray("bodies")
            val list = buildList {
                for (i in 0 until arr.length()) {
                    val p = arr.getJSONObject(i)
                    val s = p.getJSONObject("sidereal")
                    add(PlanetPosition(
                        planet = p.getString("key"),
                        longitude = s.getDouble("longitude"),
                        latitude = s.optDouble("latitude", 0.0),
                        speed = s.optDouble("speed", 0.0),
                        retrograde = s.optBoolean("retrograde", false)
                    ))
                }
            }
            val lagna = if (o.has("lagna")) {
                val l = o.getJSONObject("lagna").getJSONObject("sidereal")
                PlanetPosition("lagna", l.getDouble("longitude"))
            } else null

            ChartResponse(list, o.optInt("tithi", 1), o.optInt("weekday", 0), lagna)
        }
    }
}
