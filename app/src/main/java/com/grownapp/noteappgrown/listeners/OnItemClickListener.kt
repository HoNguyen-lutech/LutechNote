package com.grownapp.noteappgrown.listeners

import com.grownapp.noteappgrown.models.Note

interface OnItemClickListener {
    fun onNoteClick(note: Note, isChoose: Boolean)
    fun onNoteLongClick(note: Note)
}