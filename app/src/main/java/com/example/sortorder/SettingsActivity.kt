package com.example.sortorder

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Toast
import com.example.sortorder.databinding.ActivitySettingsBinding

class SettingsActivity : BaseActivity<ActivitySettingsBinding>() {

    override fun inflateBinding(layoutInflater: LayoutInflater): ActivitySettingsBinding {
        return ActivitySettingsBinding.inflate(layoutInflater)
    }

    override fun setupView() {
        val versionName = packageManager.getPackageInfo(packageName, 0).versionName ?: "1.0"
        binding.tvVersion.text = getString(R.string.version_format, versionName)

        val prefs = getSharedPreferences("game_prefs", MODE_PRIVATE)
        binding.switchSound.isChecked = prefs.getBoolean("sound_enabled", true)
        updatePrivacyOptionsVisibility()
    }

    override fun setupListeners() {
        binding.btnBack.setOnClickListener {
            AnalyticsTracker.logButton("settings", "back")
            finish()
        }

        binding.btnPremium.setOnClickListener {
            AnalyticsTracker.logButton("settings", "premium")
            AnalyticsTracker.logNavigation("premium_open")
            startActivity(Intent(this, PremiumActivity::class.java))
        }

        val prefs = getSharedPreferences("game_prefs", MODE_PRIVATE)
        binding.switchSound.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("sound_enabled", isChecked).apply()
            AnalyticsTracker.logButton("settings", "sound")
            AnalyticsTracker.logSettingsSoundToggle(isChecked)
        }

        binding.btnRate.setOnClickListener {
            AnalyticsTracker.logButton("settings", "rate")
            AnalyticsTracker.logNavigation("rate_tap")
            Toast.makeText(this, R.string.rate_thanks, Toast.LENGTH_SHORT).show()
        }

        binding.btnPolicy.setOnClickListener {
            AnalyticsTracker.logButton("settings", "policy")
            AnalyticsTracker.logNavigation("privacy_policy_open")
            openPrivacyPolicy()
        }

        binding.btnPrivacyOptions.setOnClickListener {
            AnalyticsTracker.logButton("settings", "privacy_options")
            ConsentManager.showPrivacyOptionsForm(this) {
                updatePrivacyOptionsVisibility()
                (application as? SortOrderApplication)?.initializeAdsIfAllowed()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        updatePrivacyOptionsVisibility()
    }

    private fun updatePrivacyOptionsVisibility() {
        binding.btnPrivacyOptions.visibility =
            if (ConsentManager.isPrivacyOptionsRequired(this)) View.VISIBLE else View.GONE
    }
}
