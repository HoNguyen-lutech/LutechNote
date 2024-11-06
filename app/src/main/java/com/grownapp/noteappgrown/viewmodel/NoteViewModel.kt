package com.grownapp.noteappgrown.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.grownapp.noteappgrown.models.Note
import com.grownapp.noteappgrown.repository.NoteRepository
import kotlinx.coroutines.launch

class NoteViewModel(application: Application, private val noteRepository: NoteRepository): AndroidViewModel(application) {
    fun addNote(note: Note) = viewModelScope.launch {
        noteRepository.insertNote(note)
    }
    fun deleteNote(note: Note) = viewModelScope.launch {
        noteRepository.deleteNote(note)
    }
    fun updateNote(note: Note) = viewModelScope.launch {
        noteRepository.updateNote(note)
    }
    fun getAllNotes() = noteRepository.getAllNotes()
    fun getNotesSortByTitleAToZ() = noteRepository.getNotesSortByTitleAToZ()
    fun getNotesSortByTitleZToA() = noteRepository.getNOtesSortByTitleZToA()
    fun searchNote(query: String) = noteRepository.searchNote(query)
    fun deleteNote(id: List<Int>) = viewModelScope.launch {
        noteRepository.deleteByIds(id)
    }
    fun getNotesWithoutCategory() = noteRepository.getNotesWithoutCategory()
    fun getNotesWithCategories() = noteRepository.getNotesWithCategories()
    fun getNotesByCategory(categoryId: Int) = noteRepository.getNotesByCategory(categoryId)
    fun getAllTrashNotes() = noteRepository.getAllTrashNotes()
    fun moveToTrash(id: List<Int>) = viewModelScope.launch {
        noteRepository.moveToTrash(id)
    }
    fun restoreFromTrash(id: List<Int>) = viewModelScope.launch {
        noteRepository.restoreFromTrash(id)
    }
    fun getLatestId() = noteRepository.getLatestId()
    fun getColor(id: Int) = noteRepository.getColor(id)
    fun getNoteById(id: Int) = noteRepository.getNotesById(id)
}