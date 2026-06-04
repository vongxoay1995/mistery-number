package com.example.sortorder

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity

class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_settings)

        val tvVersion = findViewById<TextView>(R.id.tvVersion)
        tvVersion.text = getString(R.string.version_format, "1.0.0")

        findViewById<View>(R.id.btnBack).setOnClickListener { finish() }

        findViewById<View>(R.id.btnPremium).setOnClickListener {
            startActivity(Intent(this, PremiumActivity::class.java))
        }

        val switchSound = findViewById<Switch>(R.id.switchSound)
        val prefs = getSharedPreferences("game_prefs", MODE_PRIVATE)
        switchSound.isChecked = prefs.getBoolean("sound_enabled", true)
        switchSound.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("sound_enabled", isChecked).apply()
        }

        findViewById<View>(R.id.btnRate).setOnClickListener {
            Toast.makeText(this, "Cảm ơn bạn!", Toast.LENGTH_SHORT).show()
        }

        findViewById<View>(R.id.btnPolicy).setOnClickListener {
            startActivity(Intent(this, PolicyActivity::class.java))
        }
    }
}
