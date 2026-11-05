package com.example.transparentweather

import android.app.*
import android.content.*
import android.media.RingtoneManager
import android.os.Build
import android.os.Bundle
import android.graphics.Color
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

class TaskAlarmReceiver:BroadcastReceiver(){
    override fun onReceive(c:Context,i:Intent){
        val id=i.getLongExtra("id",-1L)
        val task=TaskStore.all(c).firstOrNull{it.id==id} ?: return
        if(task.done) return
        if(task.repeat=="daily") AlarmPlanner.schedule(c,task)
        val channelId="task_reminders_v1"
        val notificationManager=c.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if(Build.VERSION.SDK_INT>=26){
            val ch=NotificationChannel(channelId,"Task reminders",NotificationManager.IMPORTANCE_HIGH)
            ch.enableVibration(true)
            ch.setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM),
                android.media.AudioAttributes.Builder()
                    .setUsage(android.media.AudioAttributes.USAGE_ALARM).build())
            ch.lockscreenVisibility=Notification.VISIBILITY_PUBLIC
            notificationManager.createNotificationChannel(ch)
        }
        val open=Intent(c,TaskAlertActivity::class.java).putExtra("id",id).apply{
            flags=Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pending=PendingIntent.getActivity(c,id.hashCode(),open,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val dismiss=PendingIntent.getBroadcast(c,(id.hashCode()+4000),
            Intent(c,TaskAlarmReceiver::class.java).apply{
                action="dismiss"
                data=android.net.Uri.parse("tasksreminder://dismiss/$id")
                putExtra("id",id)
            },PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        if(i.action=="dismiss"){
            notificationManager.cancel(id.hashCode())
            return
        }
        val notification=Notification.Builder(c,channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("🔔 Task reminder")
            .setContentText(task.title)
            .setStyle(Notification.BigTextStyle().bigText(task.title))
            .setCategory(Notification.CATEGORY_ALARM)
            .setPriority(Notification.PRIORITY_HIGH)
            .setVisibility(Notification.VISIBILITY_PUBLIC)
            .setContentIntent(pending)
            .setFullScreenIntent(pending,true)
            .addAction(Notification.Action.Builder(null,"Dismiss",dismiss).build())
            .setAutoCancel(true).build()
        try {notificationManager.notify(id.hashCode(),notification)}catch(_:SecurityException){}
    }
}

class TaskAlertActivity:Activity(){
    private var ringtone:android.media.Ringtone?=null
    private var id:Long=-1
    override fun onCreate(b:Bundle?){
        super.onCreate(b)
        setShowWhenLocked(true)
        setTurnScreenOn(true)
        id=intent.getLongExtra("id",-1L)
        val title=TaskStore.all(this).firstOrNull{it.id==id}?.title ?: "Task reminder"
        val layout=LinearLayout(this).apply{
            setBackgroundColor(Color.rgb(23,41,63))
            orientation=LinearLayout.VERTICAL
            gravity=Gravity.CENTER
            setPadding(28,30,28,30)
        }
        layout.addView(TextView(this).apply{
            text="🔔 REMINDER"
            textSize=28f
            gravity=Gravity.CENTER
            setTextColor(Color.WHITE)
        })
        layout.addView(TextView(this).apply{
            text=title;textSize=25f;gravity=Gravity.CENTER
            setTextColor(Color.WHITE);setPadding(0,40,0,50)
        })
        layout.addView(Button(this).apply{
            text="DISMISS"
            setOnClickListener{dismiss()}
        })
        setContentView(layout)
        try{
            ringtone=RingtoneManager.getRingtone(this,RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM))
            ringtone?.play()
        }catch(_:Exception){}
    }
    private fun dismiss(){
        stopRing()
        (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).cancel(id.hashCode())
        finish()
    }
    private fun stopRing(){try{ringtone?.stop()}catch(_:Exception){}}
    override fun onStop(){super.onStop();stopRing()}
    override fun onDestroy(){stopRing();super.onDestroy()}
}

class TasksBootReceiver:BroadcastReceiver(){
    override fun onReceive(c:Context,i:Intent){ AlarmPlanner.restore(c) }
}
