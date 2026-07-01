package com.example.sortorder

import android.view.LayoutInflater
import com.example.sortorder.databinding.ActivityPolicyBinding

class PolicyActivity : BaseActivity<ActivityPolicyBinding>() {

    override fun inflateBinding(layoutInflater: LayoutInflater): ActivityPolicyBinding {
        return ActivityPolicyBinding.inflate(layoutInflater)
    }

    override fun setupListeners() {
        binding.btnBack.setOnClickListener {
            AnalyticsTracker.logButton("policy", "back")
            finish()
        }
    }
}
