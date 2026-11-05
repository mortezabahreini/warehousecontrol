package com.example.transparentweather

import android.content.Context
import org.json.JSONObject

internal data class WeatherData(
    val city: String, val latitude: Double, val longitude: Double,
    val temperature: Int, val humidity: Int, val wind: Int,
    val high: Int, val low: Int, val weatherCode: Int,
    val isDay: Boolean, val updatedAt: Long
) {
    fun json(): String = JSONObject().apply {
        put("city", city)
        put("latitude", latitude)
        put("longitude", longitude)
        put("temperature", temperature)
        put("humidity", humidity)
        put("wind", wind)
        put("high", high)
        put("low", low)
        put("weatherCode", weatherCode)
        put("isDay", isDay)
        put("updatedAt", updatedAt)
    }.toString()
    companion object {
        fun parse(raw: String): WeatherData {
            val j = JSONObject(raw)
            return WeatherData(
                j.getString("city"), j.getDouble("latitude"), j.getDouble("longitude"),
                j.getInt("temperature"), j.getInt("humidity"), j.getInt("wind"),
                j.getInt("high"), j.getInt("low"), j.getInt("weatherCode"),
                j.getBoolean("isDay"), j.getLong("updatedAt")
            )
        }
    }
}
internal object WeatherStore {
    private const val PREFS = "weather_cache"
    fun load(c: Context): WeatherData? = try {
        c.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString("data", null)?.let(WeatherData::parse)
    } catch (_: Exception) { null }

    fun save(c: Context, d: WeatherData) {
        c.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString("data", d.json()).remove("error").apply()
    }
    fun saveError(c: Context, message: String) {
        c.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString("error", message.take(300)).apply()
    }
    fun error(c: Context): String? =
        c.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString("error", null)
}
