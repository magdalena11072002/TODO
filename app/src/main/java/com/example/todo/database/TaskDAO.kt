package com.example.todo.database

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query

@Dao
interface TaskDAO {

    @Query("SELECT * FROM tasks")
    fun getAll(): LiveData<List<TaskEntity>>

    @Insert
    suspend fun insert(task: TaskEntity)

    @Query("DELETE FROM tasks WHERE description LIKE :description AND hour LIKE :hour AND " +
            "minute LIKE :minute AND day like :day AND month LIKE :month AND year like :year")
    suspend fun delete(description: String, hour: Int, minute: Int, day: Int, month: Int, year: Int)

    @Query("SELECT * FROM tasks WHERE description LIKE :description")
    suspend fun searchTitle(description: String): List<TaskEntity>

}