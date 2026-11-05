package com.example.transparentweather

import android.app.Activity
import android.app.AlertDialog
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.graphics.Color
import android.graphics.Typeface
import android.content.Intent
import android.widget.*
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class TaskEditorActivity:Activity() {
    private lateinit var input:EditText
    private lateinit var kind:RadioGroup
    private lateinit var date:Button
    private lateinit var time:Button
    private val selected=Calendar.getInstance()
    private var existing:NoteTask?=null
    override fun onCreate(b:Bundle?) {
        super.onCreate(b)
        val px=resources.displayMetrics.density
        fun dp(n:Int)=(n*px).toInt()
        val oldId=intent.getLongExtra("id",-1L)
        existing=TaskStore.all(this).firstOrNull{it.id==oldId}
        if(existing!=null) selected.timeInMillis=existing!!.scheduled
        else selected.apply { add(Calendar.HOUR_OF_DAY,1);set(Calendar.MINUTE,0);set(Calendar.SECOND,0) }
        val root=LinearLayout(this).apply{
            orientation=LinearLayout.VERTICAL
            setPadding(dp(22),dp(30),dp(22),dp(20))
            setBackgroundColor(Color.rgb(25,44,66))
        }
        root.addView(TextView(this).apply{
            text=if(existing==null) "New task or shopping item" else "Edit item"
            textSize=23f;setTypeface(null,Typeface.BOLD);setTextColor(Color.WHITE)
        })
        input=EditText(this).apply{
            hint="What do you need to remember?"
            setSingleLine(false);minLines=2
            setTextColor(Color.WHITE);setHintTextColor(Color.LTGRAY)
            setText(existing?.title ?: "")
        }
        root.addView(input)
        root.addView(TextView(this).apply{
            text="Reminder";textSize=16f;setTextColor(Color.WHITE)
            setPadding(0,dp(12),0,dp(5))
        })
        kind=RadioGroup(this).apply{orientation=RadioGroup.VERTICAL}
        val values=listOf("once" to "At a specific date & time","daily" to "Every day at a time","none" to "No reminder")
        values.forEachIndexed{idx,(_,label)->
            kind.addView(RadioButton(this).apply{
                id=100+idx;text=label;textSize=15f;setTextColor(Color.WHITE)
            })
        }
        kind.check(100+values.indexOfFirst{it.first==(existing?.repeat ?: "once")}.coerceAtLeast(0))
        root.addView(kind)
        date=Button(this).apply{
            setOnClickListener{
                DatePickerDialog(this@TaskEditorActivity,{_,y,m,d->
                    selected.set(Calendar.YEAR,y);selected.set(Calendar.MONTH,m);selected.set(Calendar.DAY_OF_MONTH,d)
                    showSelection()
                },selected.get(Calendar.YEAR),selected.get(Calendar.MONTH),selected.get(Calendar.DAY_OF_MONTH)).show()
            }
        }
        time=Button(this).apply{
            setOnClickListener{
                TimePickerDialog(this@TaskEditorActivity,{_,h,min->
                    selected.set(Calendar.HOUR_OF_DAY,h);selected.set(Calendar.MINUTE,min)
                    showSelection()
                },selected.get(Calendar.HOUR_OF_DAY),selected.get(Calendar.MINUTE),false).show()
            }
        }
        root.addView(date);root.addView(time)
        kind.setOnCheckedChangeListener{_,id->
            date.visibility=if(id==100) android.view.View.VISIBLE else android.view.View.GONE
            time.visibility=if(id==102) android.view.View.GONE else android.view.View.VISIBLE
        }
        kind.check(kind.checkedRadioButtonId)
        showSelection()
        root.addView(Button(this).apply{
            text="SAVE ITEM & REMINDER"
            setOnClickListener{
                val title=input.text.toString().trim()
                if(title.isEmpty()){
                    Toast.makeText(this@TaskEditorActivity,"Please enter a title",Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                val mode=when(kind.checkedRadioButtonId){101->"daily";102->"none";else->"once"}
                val scheduled=selected.timeInMillis
                if(mode=="once" && scheduled<=System.currentTimeMillis()){
                    Toast.makeText(this@TaskEditorActivity,"Choose a future date and time",Toast.LENGTH_LONG).show()
                    return@setOnClickListener
                }
                val now=System.currentTimeMillis()
                val old=existing
                val task=NoteTask(old?.id ?: now,title,old?.created ?: now,false,mode,scheduled,
                    selected.get(Calendar.HOUR_OF_DAY),selected.get(Calendar.MINUTE))
                if(old!=null){
                    AlarmPlanner.cancel(this@TaskEditorActivity,old.id)
                    TaskStore.save(this@TaskEditorActivity,
                        TaskStore.all(this@TaskEditorActivity).filterNot{it.id==old.id}+task)
                }else TaskStore.add(this@TaskEditorActivity,task)
                AlarmPlanner.schedule(this@TaskEditorActivity,task)
                if(mode!="none" && !AlarmPlanner.preciseAllowed(this@TaskEditorActivity))
                    Toast.makeText(this@TaskEditorActivity,
                        "Allow Alarms & reminders in system settings for exact alerts",Toast.LENGTH_LONG).show()
                finish()
            }
        })
        root.addView(Button(this).apply{ text="CANCEL";setOnClickListener{finish()} })
        val scroll=ScrollView(this)
        scroll.addView(root);setContentView(scroll)
    }
    private fun showSelection(){
        date.text="Date: "+SimpleDateFormat("dd/MM/yyyy",Locale.getDefault()).format(selected.time)
        time.text="Time: "+SimpleDateFormat("h:mm a",Locale.getDefault()).format(selected.time)
    }
}
