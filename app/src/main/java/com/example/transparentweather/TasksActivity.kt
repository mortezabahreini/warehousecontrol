package com.example.transparentweather

import android.Manifest
import android.app.Activity
import android.app.AlarmManager
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import android.net.Uri
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TasksActivity:Activity() {
    private lateinit var list:LinearLayout
    private lateinit var permission:TextView
    private fun dp(v:Int)=(v*resources.displayMetrics.density).toInt()
    override fun onCreate(b:Bundle?) {
        super.onCreate(b)
        val root=LinearLayout(this).apply {
            orientation=LinearLayout.VERTICAL
            setPadding(dp(18),dp(20),dp(18),dp(12))
            setBackgroundColor(Color.rgb(25,44,66))
        }
        val title=TextView(this).apply {
            text="Tasks & Reminders"
            setTextColor(Color.WHITE); textSize=25f
            setTypeface(null,Typeface.BOLD)
            setPadding(0,0,0,dp(12))
        }
        root.addView(title)
        val add=Button(this).apply { text="+ Add task, shopping item or note"
            setOnClickListener { startActivity(Intent(this@TasksActivity,TaskEditorActivity::class.java)) }
        }
        root.addView(add)
        permission=TextView(this).apply {
            setTextColor(Color.rgb(247,212,139)); textSize=13f
            setPadding(dp(6),dp(12),dp(6),dp(12))
            setOnClickListener { openPermission() }
        }
        root.addView(permission)
        val scroller=ScrollView(this)
        list=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL }
        scroller.addView(list)
        root.addView(scroller,LinearLayout.LayoutParams(-1,0,1f))
        setContentView(root)
        requestNotifications()
    }
    override fun onResume() {super.onResume(); render()}
    private fun requestNotifications() {
        if(Build.VERSION.SDK_INT>=33 && !AlarmPlanner.notificationsAllowed(this))
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS),77)
    }
    private fun openPermission() {
        if(Build.VERSION.SDK_INT>=31 && !AlarmPlanner.preciseAllowed(this)) {
            startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                data=Uri.parse("package:$packageName")
            })
        } else if(!AlarmPlanner.notificationsAllowed(this)) {
            startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data=Uri.parse("package:$packageName")
            })
        }
    }
    private fun render() {
        list.removeAllViews()
        val exact=AlarmPlanner.preciseAllowed(this)
        val notifications=AlarmPlanner.notificationsAllowed(this)
        permission.text=when {
            !exact -> "⚠ Tap to allow Alarms & reminders in Android settings for exact ringing"
            !notifications -> "⚠ Tap to enable notification permission"
            else -> "✓ Reminders enabled. Tap a task to edit it."
        }
        val all=TaskStore.all(this)
        if(all.isEmpty()) {
            list.addView(TextView(this).apply {
                text="No tasks yet. Add a task above.\nYour three newest active tasks will show on the widget."
                textSize=16f; setTextColor(Color.LTGRAY)
                setPadding(dp(5),dp(30),dp(5),dp(15))
            })
        }
        all.forEach { t ->
            val row=LinearLayout(this).apply {
                orientation=LinearLayout.HORIZONTAL
                gravity=Gravity.CENTER_VERTICAL
                setPadding(dp(8),dp(7),dp(4),dp(7))
                background=GradientDrawable().apply{
                    setColor(Color.argb(100,96,123,145)); cornerRadius=dp(12).toFloat()
                }
            }
            val check=Button(this).apply {
                text=if(t.done) "✓" else "□"
                textSize=20f
                setOnClickListener { TaskStore.toggle(this@TasksActivity,t.id); render() }
            }
            row.addView(check,LinearLayout.LayoutParams(dp(54),dp(57)))
            val content=LinearLayout(this).apply {
                orientation=LinearLayout.VERTICAL
                setPadding(dp(6),0,dp(4),0)
                setOnClickListener { startActivity(Intent(this@TasksActivity,TaskEditorActivity::class.java).putExtra("id",t.id)) }
            }
            content.addView(TextView(this).apply {
                text=t.title; textSize=16f; setTextColor(Color.WHITE)
                if(t.done) paintFlags=paintFlags or android.graphics.Paint.STRIKE_THRU_TEXT_FLAG
            })
            val description=if(t.repeat=="none") "No reminder" else {
                val at=if(t.repeat=="daily") AlarmPlanner.nextDaily(t.hour,t.minute) else t.scheduled
                val formatted=SimpleDateFormat("dd/MM/yyyy h:mm a",Locale.getDefault()).format(Date(at))
                if(t.repeat=="daily") "🔔 Daily · "+
                    SimpleDateFormat("h:mm a",Locale.getDefault()).format(Date(at))
                else "🔔 $formatted"
            }
            content.addView(TextView(this).apply {
                text=description; textSize=12f; setTextColor(Color.rgb(202,222,243))
            })
            row.addView(content,LinearLayout.LayoutParams(0,dp(58),1f))
            val remove=Button(this).apply {
                text="×"; textSize=22f
                setOnClickListener {
                    AlertDialog.Builder(this@TasksActivity).setMessage("Delete this item?")
                        .setNegativeButton("Cancel",null)
                        .setPositiveButton("Delete"){ _,_ -> TaskStore.remove(this@TasksActivity,t.id);render() }.show()
                }
            }
            row.addView(remove,LinearLayout.LayoutParams(dp(52),dp(55)))
            val lp=LinearLayout.LayoutParams(-1,-2).apply{ bottomMargin=dp(8) }
            list.addView(row,lp)
        }
    }
}
