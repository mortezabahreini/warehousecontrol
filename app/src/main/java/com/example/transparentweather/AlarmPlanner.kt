package com.example.transparentweather

import android.Manifest
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import java.util.Calendar

object AlarmPlanner {
    private fun alarm(c:Context)=c.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    fun nextDaily(hour:Int,minute:Int,from:Long=System.currentTimeMillis()):Long {
        val cal=Calendar.getInstance().apply {
            timeInMillis=from
            set(Calendar.HOUR_OF_DAY,hour)
            set(Calendar.MINUTE,minute)
            set(Calendar.SECOND,0)
            set(Calendar.MILLISECOND,0)
        }
        if(cal.timeInMillis<=from) cal.add(Calendar.DAY_OF_YEAR,1)
        return cal.timeInMillis
    }
    fun intent(c:Context,id:Long):PendingIntent {
        val i=Intent(c,TaskAlarmReceiver::class.java).apply{
            action="com.morteza.tasksreminders.ALARM"
            putExtra("id",id)
            data=android.net.Uri.parse("tasksreminder://alarm/$id")
        }
        return PendingIntent.getBroadcast(c,id.hashCode(),i,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }
    fun schedule(c:Context,t:NoteTask) {
        cancel(c,t.id)
        if(t.done || t.repeat=="none") return
        val whenAt=if(t.repeat=="daily") nextDaily(t.hour,t.minute) else t.scheduled
        if(whenAt<=System.currentTimeMillis()) return
        val mgr=alarm(c)
        val pi=intent(c,t.id)
        if(Build.VERSION.SDK_INT>=31 && !mgr.canScheduleExactAlarms()) {
            // Android restricts precise alarms unless user grants the special access.
            mgr.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,whenAt,pi)
        } else {
            mgr.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,whenAt,pi)
        }
    }
    fun cancel(c:Context,id:Long) {
        alarm(c).cancel(intent(c,id))
    }
    fun restore(c:Context) {
        TaskStore.all(c).filter { !it.done }.forEach { schedule(c,it) }
        TasksWidgetProvider.render(c)
    }
    fun preciseAllowed(c:Context):Boolean = Build.VERSION.SDK_INT<31 || alarm(c).canScheduleExactAlarms()
    fun notificationsAllowed(c:Context):Boolean =
        Build.VERSION.SDK_INT<33 || c.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)==PackageManager.PERMISSION_GRANTED
}
