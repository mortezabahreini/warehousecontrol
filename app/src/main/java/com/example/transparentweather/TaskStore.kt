package com.example.transparentweather

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class NoteTask(
    val id: Long,
    val title: String,
    val created: Long,
    var done: Boolean,
    val repeat: String,
    val scheduled: Long,
    val hour: Int,
    val minute: Int
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("id",id); put("title",title); put("created",created); put("done",done)
        put("repeat",repeat); put("scheduled",scheduled); put("hour",hour); put("minute",minute)
    }
    companion object {
        fun parse(j: JSONObject) = NoteTask(
            j.getLong("id"),j.getString("title"),j.getLong("created"),j.optBoolean("done"),
            j.optString("repeat","none"),j.optLong("scheduled"),j.optInt("hour"),j.optInt("minute"))
    }
}

object TaskStore {
    private const val NAME="tasks"
    @Synchronized fun all(c:Context): List<NoteTask> {
        val raw=c.getSharedPreferences(NAME,Context.MODE_PRIVATE).getString("items","[]") ?: "[]"
        return try {
            val arr=JSONArray(raw)
            (0 until arr.length()).map { NoteTask.parse(arr.getJSONObject(it)) }
                .sortedByDescending { it.created }
        } catch (_:Exception) { emptyList() }
    }
    @Synchronized fun save(c:Context,notes:List<NoteTask>) {
        val arr=JSONArray()
        notes.forEach { arr.put(it.toJson()) }
        c.getSharedPreferences(NAME,Context.MODE_PRIVATE).edit().putString("items",arr.toString()).commit()
        TasksWidgetProvider.render(c)
    }
    @Synchronized fun add(c:Context,task:NoteTask) {
        save(c,all(c)+task)
    }
    @Synchronized fun remove(c:Context,id:Long) {
        AlarmPlanner.cancel(c,id)
        save(c,all(c).filterNot { it.id==id })
    }
    @Synchronized fun toggle(c:Context,id:Long) {
        val notes=all(c)
        val target=notes.firstOrNull { it.id==id } ?: return
        target.done=!target.done
        if(target.done) AlarmPlanner.cancel(c,id) else AlarmPlanner.schedule(c,target)
        save(c,notes)
    }
}
