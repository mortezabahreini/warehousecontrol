package com.example.transparentweather

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent

class WeatherWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(c: Context, manager: AppWidgetManager, ids: IntArray) {
        ids.forEach { WidgetRenderer.update(c, manager, it) }
        WeatherScheduler.start(c)
        WeatherScheduler.refresh(c)
    }
    override fun onEnabled(c: Context) {
        WidgetRenderer.updateAll(c)
        WeatherScheduler.start(c)
        WeatherScheduler.refresh(c)
    }
    override fun onDisabled(c: Context) { WeatherScheduler.stop(c) }

    override fun onReceive(c: Context, intent: Intent) {
        super.onReceive(c, intent)
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            WidgetRenderer.updateAll(c)
            WeatherScheduler.start(c)
            WeatherScheduler.refresh(c)
        }
    }
}
