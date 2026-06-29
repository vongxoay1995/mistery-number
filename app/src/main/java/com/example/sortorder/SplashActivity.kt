package com.example.sortorder

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.example.sortorder.databinding.ActivitySplashBinding

class SplashActivity : BaseActivity<ActivitySplashBinding>() {

    private val handler = Handler(Looper.getMainLooper())
    private var hasNavigated = false

    override fun inflateBinding(layoutInflater: LayoutInflater): ActivitySplashBinding {
        return ActivitySplashBinding.inflate(layoutInflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
    }

    override fun setupView() {
        // Pulse animation for loading dot
        val pulseAnim = AlphaAnimation(1f, 0.2f).apply {
            duration = 800
            repeatMode = Animation.REVERSE
            repeatCount = Animation.INFINITE
        }
        binding.loadingDot.startAnimation(pulseAnim)

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
