package com.grownapp.noteappgrown.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.grownapp.noteappgrown.models.NoteCategoryCrossRef
import com.grownapp.noteappgrown.repository.NoteCategoryRepository
import kotlinx.coroutines.launch

class NoteCategoryViewModel(application: Application, private val noteCategoryRepository: NoteCategoryRepository): AndroidViewModel(application) {
    fun addNoteCategory(noteCategoryCrossRef: NoteCategoryCrossRef) = viewModelScope.launch {
        noteCategoryRepository.insert(noteCategoryCrossRef)
    }
    fun addListNoteCategory(noteCategoryCrossRef: List<NoteCategoryCrossRef>) = viewModelScope.launch {
        noteCategoryRepository.insertNoteCategoryCrossRefs(noteCategoryCrossRef)
    }
    fun deleteNoteCategoryCrossRefs(id: Int) = viewModelScope.launch {
        noteCategoryRepository.deleteNoteCategoryCrossRefs(id)
    }
}