package com.grownapp.noteappgrown.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.grownapp.noteappgrown.models.NoteCategoryCrossRef

@Dao
interface NoteCategoryCrossRefDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(noteCategoryCrossRef: NoteCategoryCrossRef)
    @Transaction
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertNoteCategoryCrossRefs(noteCategoryCrossRef: List<NoteCategoryCrossRef>)
    @Transaction
    @Query("delete from note_category_cross_ref where noteId in (:id)")
    suspend fun deleteNoteCategoryCrossRefs(id: Int)
}