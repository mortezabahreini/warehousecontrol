package com.parmin.counter

import android.app.Activity
import android.app.AlertDialog
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : Activity() {
    private val storage by lazy { getSharedPreferences("parmin_counter", MODE_PRIVATE) }
    private var good = 0
    private var bad = 0
    private var start = 0L
    private lateinit var positive: TextView
    private lateinit var negative: TextView
    private lateinit var summary: TextView
    private lateinit var started: TextView

    private fun dp(value: Int) = (value * resources.displayMetrics.density + 0.5f).toInt()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        good = storage.getInt("good", 0).coerceAtLeast(0)
        bad = storage.getInt("bad", 0).coerceAtLeast(0)
        start = storage.getLong("started", 0)
        if (start <= 0) {
            start = System.currentTimeMillis()
            persist()
        }
        createScreen()
        showCounts()
    }

    private fun text(value: String, size: Float, color: Int = Color.WHITE): TextView =
        TextView(this).apply {
            text = value
            textSize = size
            setTextColor(color)
            gravity = Gravity.CENTER
        }

    private fun add(parent: LinearLayout, view: View, height: Int = -2) {
        parent.addView(view, LinearLayout.LayoutParams(-1, height).apply {
            bottomMargin = dp(12)
        })
    }

    private fun createScreen() {
        window.statusBarColor = Color.rgb(18, 24, 38)
        window.navigationBarColor = Color.rgb(18, 24, 38)
        val scroll = ScrollView(this)
        scroll.setBackgroundColor(Color.rgb(18, 24, 38))
        val column = LinearLayout(this)
        column.orientation = LinearLayout.VERTICAL
        column.setPadding(dp(20), dp(28), dp(20), dp(28))
        scroll.addView(column)
        setContentView(scroll)

        add(column, text("🌸 شمارنده پارمین", 27f))
        add(column, text("کارهای خوب و کارهای نیازمند بهبود", 15f, Color.LTGRAY))
        started = text("", 14f, Color.LTGRAY)
        add(column, started)

        val cardRow = LinearLayout(this)
        cardRow.orientation = LinearLayout.HORIZONTAL
        val goodCard = LinearLayout(this)
        goodCard.orientation = LinearLayout.VERTICAL
        goodCard.setPadding(dp(8), dp(16), dp(8), dp(16))
        goodCard.setBackgroundColor(Color.rgb(25, 78, 65))
        val badCard = LinearLayout(this)
        badCard.orientation = LinearLayout.VERTICAL
        badCard.setPadding(dp(8), dp(16), dp(8), dp(16))
        badCard.setBackgroundColor(Color.rgb(87, 43, 64))
        cardRow.addView(goodCard, LinearLayout.LayoutParams(0, -2, 1f).apply { marginEnd = dp(6) })
        cardRow.addView(badCard, LinearLayout.LayoutParams(0, -2, 1f).apply { marginStart = dp(6) })
        add(column, cardRow)

        add(goodCard, text("کارهای خوب ✅", 16f))
        positive = text("0", 48f)
        add(goodCard, positive)
        add(goodCard, Button(this).apply {
            text = "+۱ مثبت"
            isAllCaps = false
            setOnClickListener {
                if (good < Int.MAX_VALUE) good++
                persist()
                showCounts()
            }
        }, dp(54))
        add(goodCard, Button(this).apply {
            text = "−۱ اصلاح"
            isAllCaps = false
            setOnClickListener {
                if (good > 0) good--
                persist()
                showCounts()
            }
        }, dp(48))

        add(badCard, text("کارهای بد ❌", 16f))
        negative = text("0", 48f)
        add(badCard, negative)
        add(badCard, Button(this).apply {
            text = "+۱ منفی"
            isAllCaps = false
            setOnClickListener {
                if (bad < Int.MAX_VALUE) bad++
                persist()
                showCounts()
            }
        }, dp(54))
        add(badCard, Button(this).apply {
            text = "−۱ اصلاح"
            isAllCaps = false
            setOnClickListener {
                if (bad > 0) bad--
                persist()
                showCounts()
            }
        }, dp(48))

        summary = text("", 19f)
        add(column, summary)
        add(column, Button(this).apply {
            text = "ثبت نتیجه و شروع هفته جدید"
            isAllCaps = false
            setOnClickListener { confirmReset() }
        }, dp(58))
        add(column, Button(this).apply {
            text = "تاریخچه دوره‌های قبلی"
            isAllCaps = false
            setOnClickListener { showHistory() }
        }, dp(54))
        add(column, text("آمار به‌صورت خودکار داخل گوشی ذخیره می‌شود.", 13f, Color.LTGRAY))
    }

    private fun persist() {
        storage.edit()
            .putInt("good", good)
            .putInt("bad", bad)
            .putLong("started", start)
            .apply()
    }

    private fun date(timestamp: Long) =
        SimpleDateFormat("dd/MM/yyyy", Locale.US).format(Date(timestamp))

    private fun showCounts() {
        positive.text = good.toString()
        negative.text = bad.toString()
        started.text = "شروع دوره: " + date(start)
        summary.text = "مثبت: $good     منفی: $bad"
    }

    private fun history(): JSONArray = try {
        JSONArray(storage.getString("history", "[]") ?: "[]")
    } catch (_: Exception) { JSONArray() }

    private fun confirmReset() {
        AlertDialog.Builder(this)
            .setTitle("شروع دوره جدید؟")
            .setMessage("آمار فعلی: $good مثبت و $bad منفی\nنتیجه در تاریخچه ذخیره خواهد شد.")
            .setNegativeButton("لغو", null)
            .setPositiveButton("ثبت و ریست") { _, _ ->
                val archived = history()
                val entry = JSONObject()
                entry.put("start", start)
                entry.put("end", System.currentTimeMillis())
                entry.put("good", good)
                entry.put("bad", bad)
                archived.put(entry)
                val limited = JSONArray()
                for (index in maxOf(0, archived.length() - 52) until archived.length()) {
                    limited.put(archived.getJSONObject(index))
                }
                storage.edit().putString("history", limited.toString()).apply()
                good = 0
                bad = 0
                start = System.currentTimeMillis()
                persist()
                showCounts()
            }
            .show()
    }

    private fun showHistory() {
        val archived = history()
        val result = StringBuilder()
        if (archived.length() == 0) result.append("هنوز دوره‌ای ثبت نشده.")
        for (index in archived.length() - 1 downTo 0) {
            val item = archived.getJSONObject(index)
            result.append(date(item.getLong("start"))).append(" تا ")
                .append(date(item.getLong("end"))).append("\n")
                .append("مثبت: ").append(item.getInt("good"))
                .append("  منفی: ").append(item.getInt("bad")).append("\n\n")
        }
        AlertDialog.Builder(this)
            .setTitle("تاریخچه")
            .setMessage(result.toString())
            .setPositiveButton("بستن", null)
            .show()
    }
}
