package com.grownapp.noteappgrown.listeners

import android.widget.ImageView
import android.widget.TextView

interface OnColorClickListener {
    fun onColorClick(color: String)
    fun onColorClick(color: String, imageView: ImageView)
}