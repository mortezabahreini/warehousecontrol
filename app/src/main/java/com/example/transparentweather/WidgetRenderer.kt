package com.example.transparentweather

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.widget.RemoteViews

internal object WidgetRenderer {
    private val bright = Color.parseColor("#F2F4F8")

    fun updateAll(c: Context) {
        val manager = AppWidgetManager.getInstance(c)
        manager.getAppWidgetIds(ComponentName(c, WeatherWidgetProvider::class.java))
            .forEach { update(c, manager, it) }
    }

    fun update(c: Context, manager: AppWidgetManager, widgetId: Int) {
        val weather = WeatherStore.load(c)
        val views = RemoteViews(c.packageName, R.layout.widget_weather)
        val pending = PendingIntent.getActivity(
            c, 0, Intent(c, SetupActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_root, pending)
        if (weather == null) {
            views.setTextViewText(R.id.city, "· --")
            views.setTextViewText(R.id.temperature, "--°")
            views.setTextViewText(R.id.condition, "Tap to set up")
            views.setImageViewResource(R.id.weather_icon, R.drawable.ic_cloud)
            views.setTextViewText(R.id.humidity, "Humidity --")
            views.setTextViewText(R.id.wind, "Wind --")
            views.setTextViewText(R.id.high_low, "H -- · L --")
        } else {
            views.setTextViewText(R.id.city, "· " + weather.city)
            views.setTextViewText(R.id.temperature, weather.temperature.toString() + "°C")
            views.setTextViewText(R.id.condition, WeatherMapping.condition(weather.weatherCode, weather.isDay))
            views.setImageViewResource(R.id.weather_icon, WeatherMapping.icon(weather.weatherCode, weather.isDay))
            views.setTextViewText(R.id.humidity, emphasis("Humidity ", weather.humidity.toString() + "%"))
            views.setTextViewText(R.id.wind, emphasis("Wind ", weather.wind.toString() + " km/h"))
            val highText = weather.high.toString() + "°"
            val lowText = weather.low.toString() + "°"
            val hl = SpannableString("H $highText · L $lowText")
            decorate(hl, 2, 2 + highText.length)
            val lowStart = hl.toString().lastIndexOf(lowText)
            decorate(hl, lowStart, lowStart + lowText.length)
            views.setTextViewText(R.id.high_low, hl)
        }
        manager.updateAppWidget(widgetId, views)
    }

    private fun emphasis(label: String, value: String): SpannableString {
        val result = SpannableString(label + value)
        decorate(result, label.length, result.length)
        return result
    }

    private fun decorate(span: SpannableString, start: Int, end: Int) {
        span.setSpan(StyleSpan(Typeface.BOLD), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        span.setSpan(ForegroundColorSpan(bright), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
    }
}
