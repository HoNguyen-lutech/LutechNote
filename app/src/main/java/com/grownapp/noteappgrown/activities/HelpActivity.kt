package com.grownapp.noteappgrown.activities

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.grownapp.noteappgrown.R
import com.grownapp.noteappgrown.databinding.ActivityHelpBinding

class HelpActivity : AppCompatActivity() {

    private val binding: ActivityHelpBinding by lazy {
        ActivityHelpBinding.inflate(layoutInflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        binding.topAppBar.setNavigationOnClickListener { finish() }
    }
}