package com.grownapp.noteappgrown.viewmodel

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.grownapp.noteappgrown.repository.NoteCategoryRepository

class NoteCategoryViewModelFactory(private val application: Application, private val noteCategoryRepository: NoteCategoryRepository): ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return NoteCategoryViewModel(application, noteCategoryRepository) as T
    }
}