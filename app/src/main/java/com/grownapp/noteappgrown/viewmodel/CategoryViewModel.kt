package com.grownapp.noteappgrown.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.grownapp.noteappgrown.models.Category
import com.grownapp.noteappgrown.repository.CategoryRepository
import kotlinx.coroutines.launch

class CategoryViewModel(application: Application, private val categoryRepository: CategoryRepository): AndroidViewModel(application) {
    fun addCategory(category: Category) = viewModelScope.launch {
        categoryRepository.insertCategory(category)
    }
    fun deleteCategory(category: Category) = viewModelScope.launch {
        categoryRepository.deleteCategory(category)
    }
    fun updateCategory(category: Category) = viewModelScope.launch {
        categoryRepository.updateCategory(category)
    }
    fun getAllCategory() = categoryRepository.getAllCategories()
    fun getCategoryNameById(id: Int) = categoryRepository.getCategoryNameById(id)
}