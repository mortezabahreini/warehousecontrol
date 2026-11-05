package com.example.transparentweather

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.os.Build
import java.util.Locale
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

internal data class Coordinates(val lat: Double, val lon: Double)

internal object LocationHelper {
    fun allowed(c: Context): Boolean =
        c.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
        c.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED

    fun backgroundAllowed(c: Context): Boolean =
        Build.VERSION.SDK_INT < 29 ||
        c.checkSelfPermission(Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED

    @SuppressLint("MissingPermission")
    fun current(c: Context): Coordinates? {
        if (!allowed(c) || !backgroundAllowed(c)) return null
        val manager = c.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val providers = listOf(LocationManager.NETWORK_PROVIDER, LocationManager.GPS_PROVIDER)
        var latest: Location? = null
        for (provider in providers) {
            try {
                val cached = manager.getLastKnownLocation(provider)
                if (cached != null && (latest == null || cached.time > latest.time)) latest = cached
            } catch (_: Exception) {}
        }
        // WorkManager runs off the UI thread. Android 11+ supports a bounded one-shot location fix.
        if (Build.VERSION.SDK_INT >= 30) {
            for (provider in providers) {
                try {
                    if (!manager.isProviderEnabled(provider)) continue
                    val result = AtomicReference<Location?>()
                    val latch = CountDownLatch(1)
                    manager.getCurrentLocation(provider, null, { command -> command.run() }) {
                        result.set(it)
                        latch.countDown()
                    }
                    latch.await(8, TimeUnit.SECONDS)
                    result.get()?.let { return Coordinates(it.latitude, it.longitude) }
                } catch (_: Exception) {}
            }
        }
        return latest?.let { Coordinates(it.latitude, it.longitude) }
    }

    @Suppress("DEPRECATION")
    fun city(c: Context, lat: Double, lon: Double, fallback: String): String {
        if (!Geocoder.isPresent()) return fallback
        return try {
            val geocoder = Geocoder(c, Locale.getDefault())
            val address: Address? = if (Build.VERSION.SDK_INT >= 33) {
                val result = AtomicReference<Address?>()
                val latch = CountDownLatch(1)
                geocoder.getFromLocation(lat, lon, 1, object : Geocoder.GeocodeListener {
                    override fun onGeocode(addresses: MutableList<Address>) {
                        result.set(addresses.firstOrNull())
                        latch.countDown()
                    }
                    override fun onError(errorMessage: String?) { latch.countDown() }
                })
                latch.await(8, TimeUnit.SECONDS)
                result.get()
            } else {
                geocoder.getFromLocation(lat, lon, 1)?.firstOrNull()
            }
            address?.locality?.takeIf { it.isNotBlank() }
                ?: address?.subAdminArea?.takeIf { it.isNotBlank() }
                ?: address?.adminArea?.takeIf { it.isNotBlank() }
                ?: fallback
        } catch (_: Exception) { fallback }
    }
}
