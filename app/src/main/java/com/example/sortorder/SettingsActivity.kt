package com.example.sortorder

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
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
    }

    override fun setupListeners() {
        binding.btnBack.setOnClickListener { finish() }

        binding.btnPremium.setOnClickListener {
            startActivity(Intent(this, PremiumActivity::class.java))
        }

        val prefs = getSharedPreferences("game_prefs", MODE_PRIVATE)
        binding.switchSound.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("sound_enabled", isChecked).apply()
        }

        binding.btnRate.setOnClickListener {
            Toast.makeText(this, R.string.rate_thanks, Toast.LENGTH_SHORT).show()
        }

        binding.btnPolicy.setOnClickListener {
            openPrivacyPolicy()
        }
    }
}
