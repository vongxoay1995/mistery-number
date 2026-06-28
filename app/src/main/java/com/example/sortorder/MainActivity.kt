package com.example.sortorder

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.view.animation.LinearInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.FrameLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var bannerAdHelper: BannerAdHelper

    companion object {
        private const val PREF_NAME = "mystery_game_prefs"
        private const val KEY_HIGHEST_LEVEL = "highest_level_cleared"

        /**
         * Calculate total score based on highest level cleared.
         * Level 1 = 100 points
         * Level 2 = 100 + 120 = 220
         * Level 3 = 220 + 140 = 360
         * Level 4 = 360 + 160 = 520
         * ...
         * Each subsequent level adds (100 + level * 20) points
         * i.e. Lv1=100, Lv2=+120, Lv3=+140, Lv4=+160, ...
         */
        fun calculateTotalScore(highestLevelCleared: Int): Int {
            if (highestLevelCleared <= 0) return 0
            var total = 0
            for (level in 1..highestLevelCleared) {
                total += if (level == 1) 100 else (80 + level * 20)
            }
            return total
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        FullScreenHelper.apply(this)

        val tvScore = findViewById<TextView>(R.id.tvScore)
        val tvCoins = findViewById<TextView>(R.id.tvCoins)
        val btnSettings = findViewById<View>(R.id.btnSettings)
        val btnPlay = findViewById<FrameLayout>(R.id.btnPlay)
        val rippleWave1 = findViewById<View>(R.id.rippleWave1)
        val rippleWave2 = findViewById<View>(R.id.rippleWave2)
        val rippleWave3 = findViewById<View>(R.id.rippleWave3)
        val playGlow = findViewById<View>(R.id.playGlow)
        val adBanner = findViewById<FrameLayout>(R.id.adBanner)

        // Load and display score
        updateScore(tvScore)
        updateCoins(tvCoins)
        bannerAdHelper = BannerAdHelper(this, adBanner, AdMobIds.MAIN_BANNER)
        bannerAdHelper.load()

        // =============================================
        // 1. RIPPLE WAVES — 3 sonar rings, staggered
        // Each ring: scale 1→1.8, alpha 0.6→0, duration 2s
        // Staggered by 667ms each for continuous wave effect
        // =============================================
        val rippleDuration = 2000L
        val stagger = rippleDuration / 3

        fun createRippleAnimator(target: View): AnimatorSet {
            val scaleX = ObjectAnimator.ofFloat(target, "scaleX", 1f, 1.8f).apply {
                duration = rippleDuration
                repeatCount = ObjectAnimator.INFINITE
                interpolator = LinearInterpolator()
            }
            val scaleY = ObjectAnimator.ofFloat(target, "scaleY", 1f, 1.8f).apply {
                duration = rippleDuration
                repeatCount = ObjectAnimator.INFINITE
                interpolator = LinearInterpolator()
            }
            val alpha = ObjectAnimator.ofFloat(target, "alpha", 0.6f, 0f).apply {
                duration = rippleDuration
                repeatCount = ObjectAnimator.INFINITE
                interpolator = DecelerateInterpolator()
            }
            return AnimatorSet().apply { playTogether(scaleX, scaleY, alpha) }
        }

        val ripple1 = createRippleAnimator(rippleWave1)
        val ripple2 = createRippleAnimator(rippleWave2)
        val ripple3 = createRippleAnimator(rippleWave3)

        // Start ripples with stagger for sonar effect
        ripple1.start()
        rippleWave2.postDelayed({ ripple2.start() }, stagger)
        rippleWave3.postDelayed({ ripple3.start() }, stagger * 2)

        // =============================================
        // 2. GLOW BREATHING — subtle alpha pulse
        // =============================================
        ObjectAnimator.ofFloat(playGlow, "alpha", 0.3f, 0.6f).apply {
            duration = 1500
            repeatMode = ObjectAnimator.REVERSE
            repeatCount = ObjectAnimator.INFINITE
            interpolator = AccelerateDecelerateInterpolator()
            start()
        }

        // =============================================
        // 3. BUTTON BREATHING — gentle scale pulse (1.0 ↔ 1.05)
        // =============================================
        val breatheScaleX = ObjectAnimator.ofFloat(btnPlay, "scaleX", 1f, 1.05f).apply {
            duration = 1500
            repeatMode = ObjectAnimator.REVERSE
            repeatCount = ObjectAnimator.INFINITE
            interpolator = AccelerateDecelerateInterpolator()
        }
        val breatheScaleY = ObjectAnimator.ofFloat(btnPlay, "scaleY", 1f, 1.05f).apply {
            duration = 1500
            repeatMode = ObjectAnimator.REVERSE
            repeatCount = ObjectAnimator.INFINITE
            interpolator = AccelerateDecelerateInterpolator()
        }
        AnimatorSet().apply {
            playTogether(breatheScaleX, breatheScaleY)
            start()
        }

        // =============================================
        // 4. TOUCH INTERACTION
        //    Press  → scale down to 0.95 (100ms)
        //    Release → spring back to 1.0 with overshoot (300ms)
        //              then restart breathing animation
        // =============================================
        btnPlay.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    // Cancel breathing & shrink
                    breatheScaleX.cancel()
                    breatheScaleY.cancel()
                    v.animate()
                        .scaleX(0.95f)
                        .scaleY(0.95f)
                        .setDuration(100)
                        .setInterpolator(DecelerateInterpolator())
                        .start()
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    // Spring back with overshoot, then resume breathing
                    v.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(300)
                        .setInterpolator(OvershootInterpolator(2f))
                        .withEndAction {
                            breatheScaleX.start()
                            breatheScaleY.start()
                        }
                        .start()

                    if (event.action == MotionEvent.ACTION_UP) {
                        v.performClick()
                    }
                }
            }
            true
        }

        // Tap play button to start game
        btnPlay.setOnClickListener {
            startActivity(Intent(this, GameActivity::class.java))
        }

        // Settings button
        btnSettings.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

    }

    override fun onResume() {
        super.onResume()
        val tvScore = findViewById<TextView>(R.id.tvScore)
        val tvCoins = findViewById<TextView>(R.id.tvCoins)
        updateScore(tvScore)
        updateCoins(tvCoins)
        if (::bannerAdHelper.isInitialized) {
            bannerAdHelper.resume()
            bannerAdHelper.refreshVisibility()
        }
    }

    override fun onPause() {
        if (::bannerAdHelper.isInitialized) bannerAdHelper.pause()
        super.onPause()
    }

    override fun onDestroy() {
        if (::bannerAdHelper.isInitialized) bannerAdHelper.destroy()
        super.onDestroy()
    }

    private fun updateScore(tvScore: TextView) {
        val prefs = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val highestLevel = prefs.getInt(KEY_HIGHEST_LEVEL, 0)
        val totalScore = calculateTotalScore(highestLevel)
        tvScore.text = totalScore.toString()
    }

    private fun updateCoins(tvCoins: TextView) {
        tvCoins.text = CoinWallet(this).getBalance().toString()
    }
}
