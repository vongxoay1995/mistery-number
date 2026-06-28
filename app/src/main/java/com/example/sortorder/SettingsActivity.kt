package com.example.sortorder

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.widget.SwitchCompat
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        FullScreenHelper.apply(this)

        val tvVersion = findViewById<TextView>(R.id.tvVersion)
        val versionName = packageManager.getPackageInfo(packageName, 0).versionName ?: "1.0"
        tvVersion.text = getString(R.string.version_format, versionName)

        findViewById<View>(R.id.btnBack).setOnClickListener { finish() }

        findViewById<View>(R.id.btnPremium).setOnClickListener {
            startActivity(Intent(this, PremiumActivity::class.java))
        }

        val switchSound = findViewById<SwitchCompat>(R.id.switchSound)
        val prefs = getSharedPreferences("game_prefs", MODE_PRIVATE)
        switchSound.isChecked = prefs.getBoolean("sound_enabled", true)
        switchSound.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("sound_enabled", isChecked).apply()
        }

        findViewById<View>(R.id.btnRate).setOnClickListener {
            Toast.makeText(this, R.string.rate_thanks, Toast.LENGTH_SHORT).show()
        }

        findViewById<View>(R.id.btnPolicy).setOnClickListener {
            openPrivacyPolicy()
        }
    }
}
