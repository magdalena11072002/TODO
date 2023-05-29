package com.example.todo.database

import androidx.room.Entity
import androidx.room.PrimaryKey


@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val description: String,
    val group: String,
    val priority: Boolean,
    val hour: Int,
    val minute: Int,
    var day: Int,
    var month: Int,
    var year: Int,
    var done: Boolean
)