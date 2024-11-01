package com.grownapp.noteappgrown.models

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "categories")
data class Category(
    @PrimaryKey(autoGenerate = true)
    val id: Int,
    @ColumnInfo(name = "categoryName")
    val categoryName: String
)