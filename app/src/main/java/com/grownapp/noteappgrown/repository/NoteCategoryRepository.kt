package com.grownapp.noteappgrown.repository

import com.grownapp.noteappgrown.database.NoteDatabase
import com.grownapp.noteappgrown.models.NoteCategoryCrossRef

class NoteCategoryRepository(private val db: NoteDatabase) {
    suspend fun insert(noteCategoryCrossRef: NoteCategoryCrossRef) = db.noteCategoryCrossRefDao().insert(noteCategoryCrossRef)
    suspend fun insertNoteCategoryCrossRefs(noteCategoryCrossRef: List<NoteCategoryCrossRef>) = db.noteCategoryCrossRefDao().insertNoteCategoryCrossRefs(noteCategoryCrossRef)
    suspend fun deleteNoteCategoryCrossRefs(id: Int) = db.noteCategoryCrossRefDao().deleteNoteCategoryCrossRefs(id)
}