package com.parmin.counter

import android.app.Activity
import android.app.AlertDialog
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.WindowInsets
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class MainActivity : Activity() {
    private val prefs by lazy { getSharedPreferences("parmin_counter", MODE_PRIVATE) }
    private var good = 0
    private var bad = 0
    private var started = 0L

    private lateinit var goodCount: TextView
    private lateinit var badCount: TextView
    private lateinit var daysLabel: TextView
    private lateinit var periodLabel: TextView
    private lateinit var summaryLabel: TextView

    private val dark = Color.rgb(17, 24, 39)
    private val white = Color.rgb(249, 250, 251)
    private val muted = Color.rgb(169, 181, 197)
    private val green = Color.rgb(16, 185, 129)
    private val pink = Color.rgb(251, 113, 133)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        good = prefs.getInt("good", 0).coerceAtLeast(0)
        bad = prefs.getInt("bad", 0).coerceAtLeast(0)
        started = prefs.getLong("started", 0L)
        if (started == 0L) {
            started = System.currentTimeMillis()
            save()
        }
        buildInterface()
        redraw()
    }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density + 0.5f).toInt()

    private fun shape(color: Int, radius: Int = 20, border: Int? = null): GradientDrawable =
        GradientDrawable().apply {
            setColor(color)
            cornerRadius = dp(radius).toFloat()
            if (border != null) setStroke(dp(1), border)
        }

    private fun label(
        content: String, size: Float, color: Int,
        bold: Boolean = false, gravity: Int = Gravity.CENTER
    ): TextView = TextView(this).apply {
        text = content
        textSize = size
        setTextColor(color)
        this.gravity = gravity
        includeFontPadding = true
        if (bold) setTypeface(Typeface.DEFAULT, Typeface.BOLD)
    }

    private fun add(
        parent: LinearLayout, child: View,
        height: Int = -2, bottom: Int = 0
    ) {
        parent.addView(child, LinearLayout.LayoutParams(-1, height).apply {
            bottomMargin = dp(bottom)
        })
    }

    private fun buildInterface() {
        window.statusBarColor = dark
        window.navigationBarColor = dark
        val scroll = ScrollView(this).apply {
            isFillViewport = true
            setBackgroundColor(dark)
            clipToPadding = false
        }
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            setPadding(dp(18), dp(18), dp(18), dp(24))
        }
        scroll.addView(root)
        setContentView(scroll)

        // Pad around status/navigation bars on Android 15 while retaining the dark background.
        root.setOnApplyWindowInsetsListener { view, insets ->
            val bars = insets.getInsets(
                WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars()
            )
            view.setPadding(dp(18), dp(18) + bars.top, dp(18), dp(24) + bars.bottom)
            insets
        }

        add(root, label("شمارنده پارمین 🌸", 27f, white, true), bottom = 8)
        add(root, label("کارهای خوب و کارهایی که باید بهتر شوند", 14f, muted), bottom = 24)

        val timeBox = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = shape(Color.rgb(33, 43, 62), 20)
            setPadding(dp(16), dp(17), dp(16), dp(17))
        }
        periodLabel = label("", 14f, white, true)
        daysLabel = label("", 13f, muted)
        add(timeBox, periodLabel, bottom = 5)
        add(timeBox, daysLabel)
        add(root, timeBox, bottom = 18)

        val pair = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
        }

        val goodCard = countCard(
            title = "کارهای خوب",
            symbol = "✦",
            color = green,
            onIncrement = {
                if (good < Int.MAX_VALUE) good++
                save()
                redraw()
            },
            onCorrection = {
                if (good > 0) good--
                save()
                redraw()
            },
            assignCount = { goodCount = it }
        )
        val badCard = countCard(
            title = "کارهای بد",
            symbol = "!",
            color = pink,
            onIncrement = {
                if (bad < Int.MAX_VALUE) bad++
                save()
                redraw()
            },
            onCorrection = {
                if (bad > 0) bad--
                save()
                redraw()
            },
            assignCount = { badCount = it }
        )
        pair.addView(goodCard, LinearLayout.LayoutParams(0, dp(235), 1f).apply {
            marginEnd = dp(7)
        })
        pair.addView(badCard, LinearLayout.LayoutParams(0, dp(235), 1f).apply {
            marginStart = dp(7)
        })
        add(root, pair, bottom = 18)

        summaryLabel = label("", 17f, white, true)
        val summaryBox = LinearLayout(this).apply {
            background = shape(Color.rgb(33, 43, 62))
            orientation = LinearLayout.VERTICAL
            setPadding(dp(14), dp(18), dp(14), dp(18))
        }
        add(summaryBox, label("نتیجه این دوره", 13f, muted), bottom = 7)
        add(summaryBox, summaryLabel)
        add(root, summaryBox, bottom = 20)

        val reset = Button(this).apply {
            text = "↻  ثبت نتیجه و شروع دوره جدید"
            textSize = 15f
            isAllCaps = false
            setTextColor(white)
            background = shape(Color.rgb(58, 77, 103), 15)
            setOnClickListener { confirmReset() }
        }
        add(root, reset, dp(56), 11)

        val history = Button(this).apply {
            text = "مشاهده دوره‌های قبلی"
            textSize = 14f
            isAllCaps = false
            setTextColor(white)
            background = shape(Color.rgb(37, 49, 68), 15)
            setOnClickListener { showHistory() }
        }
        add(root, history, dp(52), 16)
        add(
            root,
            label(
                "هر بار که پارمین کار خوبی انجام داد، دکمه سبز را بزن. " +
                    "برای کارهای بد دکمه صورتی را بزن. آمار تا زمان ریست ذخیره می‌ماند.",
                12f, muted
            )
        )
    }

    private fun countCard(
        title: String,
        symbol: String,
        color: Int,
        onIncrement: () -> Unit,
        onCorrection: () -> Unit,
        assignCount: (TextView) -> Unit
    ): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        gravity = Gravity.CENTER
        setPadding(dp(8), dp(12), dp(8), dp(12))
        background = shape(
            if (color == green) Color.rgb(22, 69, 61) else Color.rgb(81, 43, 62),
            20, color
        )
        add(this, label(symbol, 25f, color, true), bottom = 5)
        add(this, label(title, 17f, white, true), bottom = 7)
        val number = label("0", 47f, white, true)
        assignCount(number)
        add(this, number, bottom = 5)
        val plus = Button(this@MainActivity).apply {
            text = "+۱"
            textSize = 22f
            isAllCaps = false
            setTextColor(dark)
            background = shape(color, 14)
            contentDescription = "افزودن یک مورد به " + title
            setOnClickListener { onIncrement() }
        }
        add(this, plus, dp(48), 5)
        val minus = TextView(this@MainActivity).apply {
            text = "اصلاح اشتباه: −۱"
            textSize = 11f
            setTextColor(muted)
            gravity = Gravity.CENTER
            setPadding(0, dp(5), 0, dp(5))
            contentDescription = "کم کردن یک مورد از " + title
            setOnClickListener { onCorrection() }
        }
        add(this, minus, dp(30))
    }

    private fun formatDate(whenMillis: Long): String =
        SimpleDateFormat("dd/MM/yyyy", Locale.US).format(Date(whenMillis))

    private fun redraw() {
        goodCount.text = good.toString()
        badCount.text = bad.toString()
        periodLabel.text = "شروع دوره: " + formatDate(started)
        val passedDays = TimeUnit.MILLISECONDS.toDays(
            (System.currentTimeMillis() - started).coerceAtLeast(0L)
        ) + 1L
        daysLabel.text = "روز " + passedDays + " از دوره فعلی"
        val difference = good - bad
        val sign = if (difference > 0) "+" else ""
        summaryLabel.text =
            "مثبت: " + good + "   •   منفی: " + bad + "   •   خالص: " + sign + difference
    }

    private fun save() {
        prefs.edit()
            .putInt("good", good)
            .putInt("bad", bad)
            .putLong("started", started)
            .apply()
    }

    private fun confirmReset() {
        AlertDialog.Builder(this)
            .setTitle("پایان این دوره؟")
            .setMessage(
                "نتیجه این دوره در تاریخچه ذخیره می‌شود.\n" +
                    "کارهای خوب: " + good + "\nکارهای بد: " + bad +
                    "\n\nشمارنده‌ها از صفر شروع شوند؟"
            )
            .setNegativeButton("لغو", null)
            .setPositiveButton("ذخیره و ریست") { _, _ ->
                val end = System.currentTimeMillis()
                val archive = loadHistory()
                archive.put(
                    JSONObject().apply {
                        put("start", started)
                        put("end", end)
                        put("good", good)
                        put("bad", bad)
                    }
                )
                val limited = JSONArray()
                for (i in (archive.length() - 52).coerceAtLeast(0) until archive.length()) {
                    limited.put(archive.getJSONObject(i))
                }
                prefs.edit().putString("history", limited.toString()).apply()
                good = 0
                bad = 0
                started = end
                save()
                redraw()
            }
            .show()
    }

    private fun loadHistory(): JSONArray = try {
        JSONArray(prefs.getString("history", "[]") ?: "[]")
    } catch (_: Exception) {
        JSONArray()
    }

    private fun showHistory() {
        val archive = loadHistory()
        val result = StringBuilder()
        if (archive.length() == 0) {
            result.append("هنوز دوره‌ای ثبت نشده است.")
        } else {
            for (index in archive.length() - 1 downTo 0) {
                val entry = archive.getJSONObject(index)
                result.append(formatDate(entry.getLong("start")))
                result.append(" تا ")
                result.append(formatDate(entry.getLong("end")))
                result.append("\n")
                result.append("مثبت: ")
                result.append(entry.getInt("good"))
                result.append("     منفی: ")
                result.append(entry.getInt("bad"))
                result.append("\n\n")
            }
        }
        AlertDialog.Builder(this)
            .setTitle("تاریخچه دوره‌ها")
            .setMessage(result.toString())
            .setPositiveButton("بستن", null)
            .show()
    }
}
