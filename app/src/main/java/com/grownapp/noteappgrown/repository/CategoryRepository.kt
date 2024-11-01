package com.grownapp.noteappgrown.repository

import com.grownapp.noteappgrown.database.NoteDatabase
import com.grownapp.noteappgrown.models.Category

class CategoryRepository(private val db: NoteDatabase) {
    suspend fun insertCategory(category: Category) = db.getCategoryDao().insertCategory(category)
    suspend fun deleteCategory(category: Category) = db.getCategoryDao().deleteCategory(category)
    suspend fun updateCategory(category: Category) = db.getCategoryDao().updateCategory(category)
    fun getAllCategories() = db.getCategoryDao().getAllCategories()
    fun getCategoryNameById(id: Int) = db.getCategoryDao().getCategoryNameById(id)
}