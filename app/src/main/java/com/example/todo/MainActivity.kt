package com.example.todo

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlarmManager
import android.app.AlertDialog
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Parcelable
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.PopupMenu
import android.util.Log
import kotlinx.android.synthetic.main.activity_main.*
import java.util.*
import android.os.Build
import kotlinx.android.synthetic.main.list_item.*

class MainActivity : AppCompatActivity() {

    var taskList = ArrayList<ListElement>()
    lateinit var myAdapter: MyArrayAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        famHandler()

        myAdapter = MyArrayAdapter(this, taskList)
        todolist.adapter = myAdapter

        todolist.setOnItemLongClickListener { parent, view, position, id ->
            displayOptionDialog(position)
            true
        }

        //Example task
        val element = ListElement("zadanie", "school", false, 0, 18, 27, 6, 2023, false)
        addToList(element)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle?) {
        super.onRestoreInstanceState(savedInstanceState)
        if (savedInstanceState != null) {
            taskList.clear()
            myAdapter.clear()
            val taskListSaved =
                savedInstanceState.getParcelableArrayList<ListElement>("todoList") as ArrayList<ListElement>
            taskList.addAll(taskListSaved)
            myAdapter.notifyDataSetChanged()
        }
    }

    override fun onSaveInstanceState(outState: Bundle?) {
        super.onSaveInstanceState(outState)
        outState?.putParcelableArrayList("todoList", taskList)
    }


    fun famHandler() {
        val actionButton = actionButton
        actionButton.setOnClickListener {
            val popupMenu = PopupMenu(this, it)
            popupMenu.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.action_add -> {
                        val intent = Intent(this, AddTask::class.java)
                        intent.apply { putExtra("todoList", taskList) }
                        startActivityForResult(intent, 997)
                        true
                    }
                    R.id.action_sort -> {
                        displaySortDialog()
                        true
                    }
                    else -> false
                }
            }
            popupMenu.inflate(R.menu.action_menu)
            popupMenu.show()

            try {
                val fieldMPopup = PopupMenu::class.java.getDeclaredField("mPopup")
                fieldMPopup.isAccessible = true
                val mPopup = fieldMPopup.get(popupMenu)
                mPopup.javaClass.getDeclaredMethod("setForceShowIcon", Boolean::class.java).invoke(mPopup, true)
            } catch (e: Exception) {
                Log.e("err", "Error showing menu icons.", e)
            } finally {
                popupMenu.show()
            }
        }
    }

    private fun displaySortDialog() {
        val options = arrayOf("By deadline","By group", "By priority" )
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Choose sort type")
        builder.setItems(options) { _, which ->
            val selected = options[which]
            if(selected == "By group"){
                sortByGroup()
            }else if(selected == "By priority"){
                sortByPriority()
            }
            else{
                sortByDeadLine()
            }
        }
        val dialog = builder.create()
        dialog.show()
    }

    private fun sortByPriority() {
        taskList.sortByDescending{it.priority}
        myAdapter.notifyDataSetChanged()
    }

    private fun sortByGroup(){
        taskList.sortBy { it.groupType }
        myAdapter.notifyDataSetChanged()
    }

    private fun sortByDeadLine(){
        taskList.sortWith(compareBy({it.year},{it.month},{it.day},{it.hour},{it.minute}))
        myAdapter.notifyDataSetChanged()
    }
    private fun addToList(newTodo: Parcelable) {
        val parsed: ListElement = newTodo as ListElement
        taskList.add(parsed)
        myAdapter.notifyDataSetChanged()
        scheduleNotification("Task Due", newTodo.TODO, parsed.year, parsed.month, parsed.day, parsed.hour, parsed.minute)
    }

    fun displayOptionDialog(position : Int){

        var options: Array<String>
        if(taskList[position].done){
            options = arrayOf("Mark as undone", "Remove")
        } else {
            options = arrayOf("Mark as done", "Remove")
        }

        val builder = AlertDialog.Builder(this)
        builder.setTitle("Choose action")
        builder.setItems(options) { _, which ->
            val selected = options[which]
            if(selected == "Mark as done"){
                taskList[position].done = true
                myAdapter.notifyDataSetChanged()
            } else if(selected == "Mark as undone"){
                taskList[position].done = false
                myAdapter.notifyDataSetChanged()
            }
            else{
                taskList.removeAt(position)
                myAdapter.notifyDataSetChanged()
            }
        }
        val dialog = builder.create()
        dialog.show()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == 997) {
            if (resultCode == Activity.RESULT_OK) {
                val newTodo = data!!.getParcelableExtra<ListElement>("ListItem")
                addToList(newTodo)
                myAdapter.notifyDataSetChanged()
            } else {
                super.onActivityResult(requestCode, resultCode, data)
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    @SuppressLint("UnspecifiedImmutableFlag")
    private fun scheduleNotification(title: String, message: String, year: Int, month: Int, day: Int, hour: Int, minute: Int) {
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val notificationIntent = Intent(this, NotificationReceiver::class.java).apply {
            action = "ACTION_SHOW_NOTIFICATION"
            putExtra("title", title)
            putExtra("message", message)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            this,
            0,
            notificationIntent,
            PendingIntent.FLAG_UPDATE_CURRENT
        )

        val calendar = Calendar.getInstance()
        calendar.set(Calendar.YEAR, year)
        calendar.set(Calendar.MONTH, month - 1)
        calendar.set(Calendar.DAY_OF_MONTH, day)
        calendar.set(Calendar.HOUR_OF_DAY, hour)
        calendar.set(Calendar.MINUTE, minute)
        calendar.set(Calendar.SECOND, 0)

        val notificationTimeInMillis = calendar.timeInMillis

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                notificationTimeInMillis,
                pendingIntent
            )
        } else {
            alarmManager.setExact(
                AlarmManager.RTC_WAKEUP,
                notificationTimeInMillis,
                pendingIntent
            )
        }
    }

}
