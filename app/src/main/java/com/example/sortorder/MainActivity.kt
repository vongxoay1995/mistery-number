package com.example.sortorder

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        val tvTapToPlay = findViewById<TextView>(R.id.tvTapToPlay)
        val btnSettings = findViewById<ImageView>(R.id.btnSettings)

        // Pulse animation for "Tap to play"
        val pulseAnim = AlphaAnimation(1f, 0.4f).apply {
            duration = 1200
            repeatMode = Animation.REVERSE
            repeatCount = Animation.INFINITE
        }
        tvTapToPlay.startAnimation(pulseAnim)

        // Tap anywhere (except settings) to start game
        findViewById<View>(R.id.main).setOnClickListener {
            startActivity(Intent(this, GameActivity::class.java))
        }

        // Settings button
        btnSettings.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
    }
}