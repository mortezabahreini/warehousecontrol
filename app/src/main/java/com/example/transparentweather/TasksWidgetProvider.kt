package com.example.transparentweather

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.View
import android.widget.RemoteViews
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TasksWidgetProvider:AppWidgetProvider() {
    override fun onUpdate(c:Context,m:AppWidgetManager,ids:IntArray) { render(c) }
    override fun onReceive(c:Context,i:Intent) {
        super.onReceive(c,i)
        val id=i.getLongExtra("id",-1L)
        if(id!=-1L) when(i.action) {
            ACTION_DELETE -> TaskStore.remove(c,id)
            ACTION_TOGGLE -> TaskStore.toggle(c,id)
        }
    }
    companion object {
        const val ACTION_DELETE="com.morteza.tasksreminders.DELETE"
        const val ACTION_TOGGLE="com.morteza.tasksreminders.TOGGLE"
        private val titles=intArrayOf(R.id.title1,R.id.title2,R.id.title3)
        private val details=intArrayOf(R.id.detail1,R.id.detail2,R.id.detail3)
        private val whens=intArrayOf(R.id.when1,R.id.when2,R.id.when3)
        private val checks=intArrayOf(R.id.check1,R.id.check2,R.id.check3)
        private val deletes=intArrayOf(R.id.delete1,R.id.delete2,R.id.delete3)
        private val rows=intArrayOf(R.id.task1,R.id.task2,R.id.task3)
        private fun tap(c:Context,clazz:Class<*>,request:Int):PendingIntent =
            PendingIntent.getActivity(c,request,Intent(c,clazz),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        private fun action(c:Context,id:Long,kind:String):PendingIntent {
            val i=Intent(c,TasksWidgetProvider::class.java).apply {
                action=kind
                data=Uri.parse("taskwidget://$kind/$id")
                putExtra("id",id)
            }
            return PendingIntent.getBroadcast(c,(id.toInt()*31+kind.hashCode()),i,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        }
        fun render(c:Context) {
            val manager=AppWidgetManager.getInstance(c)
            val ids=manager.getAppWidgetIds(ComponentName(c,TasksWidgetProvider::class.java))
            val notes=TaskStore.all(c).filter{!it.done}.take(3)
            for(widgetId in ids) {
                val r=RemoteViews(c.packageName,R.layout.widget_tasks)
                r.setOnClickPendingIntent(R.id.card,tap(c,TasksActivity::class.java,0))
                r.setOnClickPendingIntent(R.id.add,tap(c,TaskEditorActivity::class.java,1))
                for(index in 0..2) {
                    val n=notes.getOrNull(index)
                    r.setViewVisibility(rows[index],if(n==null) View.GONE else View.VISIBLE)
                    if(n!=null) {
                        r.setTextViewText(titles[index],n.title)
                        val label=when(n.repeat) {
                            "daily" -> "Daily"
                            "once" -> SimpleDateFormat("dd/MM/yyyy",Locale.getDefault()).format(Date(n.scheduled))
                            else -> "No reminder"
                        }
                        val time=if(n.repeat=="none") "" else
                            SimpleDateFormat("h:mm a",Locale.getDefault()).format(Date(
                                if(n.repeat=="daily") AlarmPlanner.nextDaily(n.hour,n.minute) else n.scheduled))
                        r.setTextViewText(details[index], if(n.repeat=="none") "Just a note" else "🔔 $label, $time")
                        r.setTextViewText(whens[index],if(n.repeat=="none") "" else "$label\n$time")
                        r.setOnClickPendingIntent(checks[index],action(c,n.id,ACTION_TOGGLE))
                        r.setOnClickPendingIntent(deletes[index],action(c,n.id,ACTION_DELETE))
                    }
                }
                manager.updateAppWidget(widgetId,r)
            }
        }
    }
}
