package com.grownapp.noteappgrown.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.grownapp.noteappgrown.models.Category
import com.grownapp.noteappgrown.models.Note
import com.grownapp.noteappgrown.models.NoteCategoryCrossRef

@Database(entities = [Note::class, Category::class, NoteCategoryCrossRef::class], version = 1, exportSchema = true)
abstract class NoteDatabase : RoomDatabase(){
    abstract fun getNoteDao(): NoteDao
    abstract fun getCategoryDao(): CategoryDao
    abstract fun noteCategoryCrossRefDao(): NoteCategoryCrossRefDao
    companion object{
        @Volatile
        private var instance: NoteDatabase? = null
        private var LOCK = Any()
        operator fun invoke(context: Context) = instance ?: synchronized(LOCK){
            instance ?: createDatabase(context).also {
                instance = it
            }
        }
        private fun createDatabase(context: Context) = Room.databaseBuilder(context.applicationContext, NoteDatabase::class.java, "note_db")
            .fallbackToDestructiveMigration()
            .allowMainThreadQueries()
            .build()
    }
}