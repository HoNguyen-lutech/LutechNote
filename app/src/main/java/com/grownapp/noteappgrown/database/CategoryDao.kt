package com.grownapp.noteappgrown.database

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.grownapp.noteappgrown.models.Category

@Dao
interface CategoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: Category)
    @Delete
    suspend fun deleteCategory(category: Category)
    @Update
    suspend fun updateCategory(category: Category)
    @Query("select * from categories order by id desc")
    fun getAllCategories(): LiveData<List<Category>>

    @Query("select categories.categoryName " +
            "from categories " +
            "left join note_category_cross_ref " +
            "on categories.id = note_category_cross_ref.categoryId" +
            " where noteId = :id")
    fun getCategoryNameById(id: Int): List<String>
}