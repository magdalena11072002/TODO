package com.example.todo.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [TaskEntity::class], version = 1)
abstract class MyDB : RoomDatabase() {

    companion object{
        private var INSTANCE: MyDB? = null

        fun get(context: Context): MyDB{
            if(INSTANCE == null) {
                INSTANCE = Room.databaseBuilder(context, MyDB::class.java, "tasks").build()
            }
            return INSTANCE as MyDB
        }

    }
    abstract fun TaskDAO(): TaskDAO
}