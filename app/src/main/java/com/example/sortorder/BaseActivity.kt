package com.example.sortorder

import android.os.Bundle
import android.view.LayoutInflater
import androidx.appcompat.app.AppCompatActivity
import androidx.viewbinding.ViewBinding

abstract class BaseActivity<VB : ViewBinding> : AppCompatActivity() {

    protected lateinit var binding: VB

    abstract fun inflateBinding(layoutInflater: LayoutInflater): VB

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = inflateBinding(layoutInflater)
        setContentView(binding.root)
        FullScreenHelper.apply(this)
        
        setupView()
        setupListeners()
        setupObservers()
    }

    override fun onResume() {
        super.onResume()
        val screenName = getAnalyticsScreenName()
        AnalyticsTracker.logScreen(
            screenName = screenName.replaceFirstChar { it.uppercase() },
            screenClass = javaClass.simpleName
        )
        AnalyticsTracker.logScreenShow(screenName)
    }

    open fun setupView() {}
    open fun setupListeners() {}
    open fun setupObservers() {}

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            FullScreenHelper.apply(this)
        }
    }

    private fun getAnalyticsScreenName(): String {
        return javaClass.simpleName
            .removeSuffix("Activity")
            .replace(Regex("([a-z])([A-Z])"), "$1_$2")
            .lowercase()
    }
}
