package com.example.sortorder

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen

class SplashActivity : AppCompatActivity() {

    private val handler = Handler(Looper.getMainLooper())
    private var hasNavigated = false

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)
        FullScreenHelper.apply(this)

        // Pulse animation for loading dot
        val loadingDot = findViewById<android.view.View>(R.id.loadingDot)
        val pulseAnim = AlphaAnimation(1f, 0.2f).apply {
            duration = 800
            repeatMode = Animation.REVERSE
            repeatCount = Animation.INFINITE
        }
        loadingDot.startAnimation(pulseAnim)

        handler.postDelayed({
            showAppOpenAdThenNavigate()
        }, 2000)
    }

    private fun showAppOpenAdThenNavigate() {
        val sortOrderApplication = application as? SortOrderApplication
        if (sortOrderApplication == null) {
            navigateToMain()
        } else {
            sortOrderApplication.showAppOpenAdIfAvailable(this) {
                navigateToMain()
            }
        }
    }

    private fun navigateToMain() {
        if (hasNavigated) return
        hasNavigated = true
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }
}
