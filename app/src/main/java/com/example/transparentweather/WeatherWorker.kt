package com.example.transparentweather

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class WeatherWorker(c: Context, p: WorkerParameters) : CoroutineWorker(c, p) {
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val c = applicationContext
        try {
            val old = WeatherStore.load(c)
            val current = LocationHelper.current(c)
            val coordinates = current ?: old?.let { Coordinates(it.latitude, it.longitude) }
            if (coordinates == null) {
                WeatherStore.saveError(c, "No location. Grant location and Allow all the time.")
                WidgetRenderer.updateAll(c)
                return@withContext Result.success()
            }
            val previous = old?.city ?: "Current location"
            val city = if (current != null) {
                LocationHelper.city(c, coordinates.lat, coordinates.lon, previous)
            } else previous
            WeatherStore.save(c, WeatherApi.fetch(coordinates.lat, coordinates.lon, city))
            WidgetRenderer.updateAll(c)
            Result.success()
        } catch (e: Exception) {
            WeatherStore.saveError(c, e.message ?: e.javaClass.simpleName)
            WidgetRenderer.updateAll(c)
            Result.success()
        }
    }
}
