package com.example.transparentweather

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.text.format.DateFormat
import android.widget.Button
import android.widget.TextView
import java.util.Date

class SetupActivity : Activity() {
    private lateinit var status: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_setup)
        status = findViewById(R.id.status)
        findViewById<Button>(R.id.grant_location).setOnClickListener {
            if (LocationHelper.allowed(this)) showStatus()
            else requestPermissions(arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION), 101)
        }
        findViewById<Button>(R.id.background_location).setOnClickListener {
            startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:$packageName")
            })
        }
        findViewById<Button>(R.id.refresh).setOnClickListener {
            WeatherScheduler.refresh(this)
            status.text = "Refresh requested.\n\n" + statusDetails()
        }
        WeatherScheduler.start(this)
        WeatherScheduler.refresh(this)
        showStatus()
    }

    override fun onResume() {
        super.onResume()
        if (::status.isInitialized) showStatus()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 101 && grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED)
            WeatherScheduler.refresh(this)
        showStatus()
    }

    private fun showStatus() { status.text = statusDetails() }

    private fun statusDetails(): String {
        val location = if (LocationHelper.allowed(this)) "Granted" else "Not granted"
        val background = if (LocationHelper.backgroundAllowed(this)) "Granted" else "Not granted"
        val data = WeatherStore.load(this)
        val updated = data?.let {
            DateFormat.format("dd/MM/yyyy hh:mm a", Date(it.updatedAt)).toString() +
                " (" + it.city + ", " + it.temperature + "°C)"
        } ?: "No weather received"
        val error = WeatherStore.error(this)
        return "Location: $location\nBackground location: $background\n\nLast update: $updated" +
            (if (error != null) "\nLast error: $error" else "") +
            (if (!LocationHelper.backgroundAllowed(this))
                "\n\nIn Android settings, open Permissions > Location > Allow all the time."
            else "")
    }
}
