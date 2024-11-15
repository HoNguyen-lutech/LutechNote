package com.grownapp.noteappgrown.activities

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.grownapp.noteappgrown.R
import com.grownapp.noteappgrown.databinding.ActivityBackupBinding

class BackupActivity : AppCompatActivity() {

    private val binding: ActivityBackupBinding by lazy {
        ActivityBackupBinding.inflate(layoutInflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        binding.topAppBar.setNavigationOnClickListener { finish() }
        binding.showInstructionsBtn.setOnClickListener {
            startActivity(Intent(this, HelpActivity::class.java))
            finish()
        }
    }
}