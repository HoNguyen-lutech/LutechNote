package com.grownapp.noteappgrown.viewmodel

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.grownapp.noteappgrown.repository.CategoryRepository

class CategoryViewModelFactory(private val application: Application, private val categoryRepository: CategoryRepository): ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return CategoryViewModel(application, categoryRepository) as T
    }
}