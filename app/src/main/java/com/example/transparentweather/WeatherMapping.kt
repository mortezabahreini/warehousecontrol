package com.example.transparentweather

internal object WeatherMapping {
    fun condition(code: Int, day: Boolean): String = when (code) {
        0 -> if (day) "Clear" else "Clear night"
        1 -> "Mainly clear"
        2 -> "Partly cloudy"
        3 -> "Overcast"
        45, 48 -> "Fog"
        in 51..57 -> "Drizzle"
        61 -> "Light rain"
        63 -> "Rain"
        in 65..67 -> "Heavy rain"
        in 71..77 -> "Snow"
        in 80..82 -> "Showers"
        85, 86 -> "Snow showers"
        in 95..99 -> "Thunderstorm"
        else -> "Weather"
    }

    fun icon(code: Int, day: Boolean): Int = when (code) {
        0, 1 -> if (day) R.drawable.ic_sun else R.drawable.ic_moon
        2 -> R.drawable.ic_partly_cloudy
        3 -> R.drawable.ic_cloud
        45, 48 -> R.drawable.ic_fog
        in 51..57, in 61..67, in 80..82 -> R.drawable.ic_rain
        in 71..77, 85, 86 -> R.drawable.ic_snow
        in 95..99 -> R.drawable.ic_thunderstorm
        else -> R.drawable.ic_cloud
    }
}
