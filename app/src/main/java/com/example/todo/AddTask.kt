package com.example.todo

import android.app.Activity
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.lifecycleScope
import com.example.todo.database.MyDB
import com.example.todo.database.TaskEntity
import com.example.todo.databinding.ActivityAddTaskBinding
import com.example.todo.databinding.ActivityMainBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.threeten.bp.LocalDate
import org.threeten.bp.format.DateTimeFormatter

import java.util.*

class AddTask : AppCompatActivity(), LifecycleOwner {
    private lateinit var lifecycleRegistry: LifecycleRegistry
    private lateinit var binding: ActivityAddTaskBinding

    override fun getLifecycle(): Lifecycle {
        if (!::lifecycleRegistry.isInitialized) {
            lifecycleRegistry = LifecycleRegistry(this)
        }
        return lifecycleRegistry
    }

    lateinit var date : String
    var day = 0
    var month =0
    var year =0
    var group = "none"
    var hour = 0
    var minute = 0
    var priority = false
    val calendar = Calendar.getInstance()!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddTaskBinding.inflate(layoutInflater)
        setContentView(binding.root)
        year = calendar.get(Calendar.YEAR)
        month = calendar.get(Calendar.MONTH)+1
        day = calendar.get(Calendar.DAY_OF_MONTH)
        hour = calendar.get(Calendar.HOUR_OF_DAY)
        minute = calendar.get(Calendar.MINUTE)

        binding.dateTF.setText(displayCorrectDate(day,month,year))
        binding.timeTF.setText(displayCorrectTime(hour,minute))

        binding.pickTimeButton.setOnClickListener{

            val timePicker = TimePickerDialog(this,TimePickerDialog.OnTimeSetListener { view, mHour, mMinute ->
                hour = mHour
                minute = mMinute
                binding.timeTF.setText(displayCorrectTime(hour,minute))
            }, minute,hour, true)
            timePicker.show()
        }
        binding.priorityB.setOnCheckedChangeListener { buttonView, isChecked ->
            priority = isChecked
        }
        binding.pickDateButton.setOnClickListener {
            val dpd1 = DatePickerDialog(this, DatePickerDialog.OnDateSetListener{ view, mYear, mMonth, mDay ->
                day = mDay
                month = mMonth+1
                year = mYear
                binding.dateTF.setText(displayCorrectDate(mDay,mMonth+1,mYear))
            }, year, month, day)
            dpd1.show()
        }
    }
    fun createTODO (view: View){

        if(group.isEmpty()){
            Toast.makeText(this@AddTask, "Select group!", Toast.LENGTH_SHORT).show()
            return
        }

        val todo = binding.todoTF.text.toString()
        date =  binding.dateTF.text.toString()
        val time = binding.timeTF.text.toString()
        val listElement = ListElement(todo, group, priority, minute,hour,day,month,year, false)

        val newTask = TaskEntity(description = todo, group = group, priority = priority, hour = hour,minute = minute, day = day, month = month, year = year, done =
        false)

        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                val db = MyDB.get(applicationContext)
                db.TaskDAO().insert(newTask)
            }
        }

        val intent = Intent(this, MainActivity::class.java)
        intent.apply { putExtra("ListItem", listElement)}
        setResult(Activity.RESULT_OK,intent)
        finish()
    }

    fun addGroup(view:View){
        val btn: ImageButton = findViewById(view.id)
        when (btn) {
            binding.workGroup -> group = "work"
            binding.chillGroup -> group = "chill"
            binding.schoolGroup -> group = "school"
            binding.homeGroup -> group = "home"
        }
    }

    fun todayClick(view: View){
        val today = LocalDate.now()
        val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
        val formatted = today.format(formatter);
        date = formatted.toString()
        binding.dateTF.setText(date)
    }

    fun plusHourClick(view: View){
        hour++

        if(hour>23){
            hour =0
        }

        binding.timeTF.setText(displayCorrectTime(hour,minute))
    }
    fun displayCorrectTime(hour : Int,minute : Int): String{

        var minuteString = minute.toString()
        var hourString = hour.toString()

        if(minute<10){
            minuteString = "0$minute"
        }
        when {
            hour<10 -> hourString = "0$hour"
            hour>23 -> {
                hourString = "00"
            }
        }
        return "$hourString:$minuteString"
    }

    fun displayCorrectDate(day : Int, month: Int, year : Int) : String{

        var dayString = day.toString()
        var monthString = month.toString()

        if(day<10){
            dayString = "0$dayString"
        }
        if(month<10){
            monthString = "0$month"
        }

        return "$dayString/$monthString/$year"
    }
}
