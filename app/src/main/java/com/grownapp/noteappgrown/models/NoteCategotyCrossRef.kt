package com.grownapp.noteappgrown.models

import androidx.room.Entity
import androidx.room.ForeignKey

@Entity(
    tableName = "note_category_cross_ref",
    primaryKeys = ["noteId", "categoryId"],
    foreignKeys = [
        ForeignKey(
            entity = Note::class,
            parentColumns = ["id"],
            childColumns = ["noteId"],
            onDelete = ForeignKey.CASCADE
        ),
    ForeignKey(
        entity = Category::class,
        parentColumns =  ["id"],
        childColumns = ["categoryId"],
        onDelete = ForeignKey.CASCADE
    )
    ]
)
data class NoteCategoryCrossRef (
    val noteId: Int,
    val categoryId: Int
)