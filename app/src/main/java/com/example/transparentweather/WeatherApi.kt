package com.example.transparentweather

import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
import kotlin.math.roundToInt

internal object WeatherApi {
    fun fetch(lat: Double, lon: Double, city: String): WeatherData {
        val latitude = String.format(Locale.US, "%.5f", lat)
        val longitude = String.format(Locale.US, "%.5f", lon)
        val url = URL(
            "https://api.open-meteo.com/v1/forecast?latitude=$latitude&longitude=$longitude" +
                "&current=temperature_2m,relative_humidity_2m,weather_code,wind_speed_10m,is_day" +
                "&daily=temperature_2m_max,temperature_2m_min" +
                "&temperature_unit=celsius&wind_speed_unit=kmh&timezone=auto&forecast_days=1"
        )
        val conn = url.openConnection() as HttpURLConnection
        try {
            conn.connectTimeout = 15000
            conn.readTimeout = 15000
            conn.setRequestProperty("Accept", "application/json")
            if (conn.responseCode !in 200..299)
                throw IllegalStateException("Weather HTTP " + conn.responseCode)
            val json = JSONObject(conn.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() })
            val current = json.getJSONObject("current")
            val daily = json.getJSONObject("daily")
            return WeatherData(
                city, lat, lon,
                current.getDouble("temperature_2m").roundToInt(),
                current.getDouble("relative_humidity_2m").roundToInt(),
                current.getDouble("wind_speed_10m").roundToInt(),
                daily.getJSONArray("temperature_2m_max").getDouble(0).roundToInt(),
                daily.getJSONArray("temperature_2m_min").getDouble(0).roundToInt(),
                current.getInt("weather_code"),
                current.getInt("is_day") == 1,
                System.currentTimeMillis()
            )
        } finally {
            conn.disconnect()
        }
    }
}
