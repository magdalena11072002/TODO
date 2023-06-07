package com.example.todo

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlarmManager
import android.app.AlertDialog
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.os.Parcelable
import android.util.Log
import com.example.todo.databinding.ActivityMainBinding
import java.util.*
import android.os.Build
import android.text.Editable
import android.text.TextWatcher
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.PopupMenu
import androidx.lifecycle.lifecycleScope
import com.example.todo.database.MyDB
import com.example.todo.database.TaskDAO
import com.example.todo.database.TaskEntity
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext



class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private val taskList = ArrayList<ListElement>()
    private lateinit var myAdapter: MyArrayAdapter
    private lateinit var db : MyDB

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        db = MyDB.get(this)
        famHandler()
        // Inicjalizacja adaptera i przypisanie go do listy
        myAdapter = MyArrayAdapter(this, taskList)
        binding.todolist.adapter = myAdapter
        val taskDao = db.TaskDAO()

        binding.buttonSearch.setOnClickListener {
            if (!binding.search.text.isEmpty()) {
                lifecycleScope.launch {
                    val searchTerm = "%${binding.search.text.toString()}%"
                    val tasks = withContext(Dispatchers.IO) {
                        taskDao.searchTitle(searchTerm)
                    }
                    taskList.clear()
                    taskList.addAll(convertTaskEntitiesToListElements(tasks))
                    myAdapter.notifyDataSetChanged()
                }
            }
        }


        binding.search.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                val searchTerm = s.toString()

                // Przeszukaj bazę danych
                val searchResults = runBlocking {
                    taskDao.searchTitle("%$searchTerm%")
                }

                // Zaktualizuj listę o wyniki wyszukiwania
                myAdapter.notifyDataSetChanged()
            }

            override fun afterTextChanged(s: Editable?) {
                val searchTerm = s.toString()
                if (searchTerm.isEmpty()) {
                    lifecycleScope.launch {
                        withContext(Dispatchers.Main) {
                            val tasks = withContext(Dispatchers.IO) {
                                db.TaskDAO().getAll()
                            }
                            tasks.observe(this@MainActivity) { taskEntities ->
                                taskList.clear()
                                taskList.addAll(convertTaskEntitiesToListElements(taskEntities))
                                myAdapter.notifyDataSetChanged()
                            }
                        }
                    }
                }
            }
        })



        // Ustawienie nasłuchiwania na długie kliknięcie elementu na liście
        binding.todolist.setOnItemLongClickListener { parent, view, position, id ->
            displayOptionDialog(position)
            true
        }
        // Inicjalizacja listy zadań z bazy danych
        lifecycleScope.launch {
            withContext(Dispatchers.Main) {
                val tasks = withContext(Dispatchers.IO) {
                    db.TaskDAO().getAll()
                }
                tasks.observe(this@MainActivity) { taskEntities ->
                    taskList.clear()
                    taskList.addAll(convertTaskEntitiesToListElements(taskEntities))
                    myAdapter.notifyDataSetChanged()
                }
            }
        }
        // Rejestracja odbiornika aktualizującego adapter
        val filter = IntentFilter("UPDATE_ADAPTER")
        registerReceiver(updateAdapterReceiver, filter)

    }
    // Odbiornik aktualizujący adapter
    private val updateAdapterReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            myAdapter.notifyDataSetChanged()
        }
    }

    override fun onDestroy() {
        unregisterReceiver(updateAdapterReceiver)
        super.onDestroy()
    }
    // Konwersja obiektów TaskEntity na ListElement
    private fun convertTaskEntitiesToListElements(tasks: List<TaskEntity>): List<ListElement> {
        return tasks.map { task ->
            ListElement(
                task.description,
                task.group,
                task.priority,
                task.hour,
                task.minute,
                task.day,
                task.month,
                task.year,
                task.done
            )
        }
    }
    // Przywracanie stanu listy po zmianie orientacji ekranu
    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
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
    // Zapisywanie stanu listy przed zmianą orientacji ekranu
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putParcelableArrayList("todoList", taskList)
    }

    // Obsługa przycisku FloatingActionButton
    fun famHandler() {
        val actionButton: FloatingActionButton = findViewById(R.id.actionButton)
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

    // Wyświetlanie dialogu sortowania
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
    // Sortowanie według priorytetu
    private fun sortByPriority() {
        taskList.sortByDescending{it.priority}
        myAdapter.notifyDataSetChanged()
    }
    // Sortowanie według grupy
    private fun sortByGroup(){
        taskList.sortBy { it.groupType }
        myAdapter.notifyDataSetChanged()
    }
    // Sortowanie według terminu
    private fun sortByDeadLine(){
        taskList.sortWith(compareBy({it.year},{it.month},{it.day},{it.hour},{it.minute}))
        myAdapter.notifyDataSetChanged()
    }
    // Dodawanie elementu do listy
    private fun addToList(newTodo: Parcelable) {
        val parsed: ListElement = newTodo as ListElement
        taskList.add(parsed)
        myAdapter.notifyDataSetChanged()
        scheduleNotification("Task Due", newTodo.TODO, parsed.year, parsed.month, parsed.day, parsed.hour, parsed.minute)
    }
    // Wyświetlanie dialogu opcji dla elementu na liście
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
            if (selected == "Mark as done") {
                taskList[position].done = true
                lifecycleScope.launch {
                    withContext(Dispatchers.IO) {
                        db = MyDB.get(applicationContext)
                        val task = convertListElementToTaskEntity(taskList[position])
                        db.TaskDAO().done_true(task.description, task.hour, task.minute, task.day, task.month, task.year)
                    }
                }
                myAdapter.notifyDataSetChanged()
            } else if (selected == "Mark as undone") {
                taskList[position].done = false
                lifecycleScope.launch {
                    withContext(Dispatchers.IO) {
                        db = MyDB.get(applicationContext)
                        val task = convertListElementToTaskEntity(taskList[position])
                        db.TaskDAO().done_false(task.description, task.hour, task.minute, task.day, task.month, task.year)
                    }
                }
                myAdapter.notifyDataSetChanged()
            } else {
                lifecycleScope.launch {
                    withContext(Dispatchers.IO) {
                        db = MyDB.get(applicationContext)
                        val task = convertListElementToTaskEntity(taskList[position])
                        db.TaskDAO().delete(task.description, task.hour, task.minute, task.day, task.month, task.year)

                        // Remove the task from the list
                        taskList.removeAt(position)
                    }
                }
                myAdapter.notifyDataSetChanged()

            }
        }
        val dialog = builder.create()
        dialog.show()
    }
    // Konwersja obiektu ListElement na obiekt TaskEntity
    private fun convertListElementToTaskEntity(listElement: ListElement): TaskEntity {
        return TaskEntity(
            description = listElement.TODO,
            group = listElement.groupType,
            priority = listElement.priority,
            hour = listElement.hour,
            minute = listElement.minute,
            day = listElement.day,
            month = listElement.month,
            year = listElement.year,
            done = listElement.done
        )
    }

    // Obsługa wyniku z Activity dodawania zadania
   override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == 997) {
            if (resultCode == Activity.RESULT_OK) {
                val newTodo = data!!.getParcelableExtra<ListElement>("ListItem")
                if (newTodo != null) {
                    addToList(newTodo)
                }
                myAdapter.notifyDataSetChanged()
            } else {
                super.onActivityResult(requestCode, resultCode, data)
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }
    // Harmonogramowanie powiadomienia
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
            PendingIntent.FLAG_UPDATE_CURRENT or  PendingIntent.FLAG_IMMUTABLE
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