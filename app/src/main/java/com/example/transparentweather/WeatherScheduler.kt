package com.example.transparentweather

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

internal object WeatherScheduler {
    private const val PERIODIC = "weather_periodic"
    private const val MANUAL = "weather_manual"

    fun start(c: Context) {
        val request = PeriodicWorkRequestBuilder<WeatherWorker>(30, TimeUnit.MINUTES)
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .build()
        WorkManager.getInstance(c.applicationContext)
            .enqueueUniquePeriodicWork(PERIODIC, ExistingPeriodicWorkPolicy.KEEP, request)
    }

    fun refresh(c: Context) {
        val request = OneTimeWorkRequestBuilder<WeatherWorker>()
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .build()
        WorkManager.getInstance(c.applicationContext)
            .enqueueUniqueWork(MANUAL, ExistingWorkPolicy.REPLACE, request)
    }

    fun stop(c: Context) {
        WorkManager.getInstance(c.applicationContext).cancelUniqueWork(PERIODIC)
        WorkManager.getInstance(c.applicationContext).cancelUniqueWork(MANUAL)
    }
}
