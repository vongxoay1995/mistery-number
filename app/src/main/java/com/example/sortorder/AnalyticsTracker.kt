package com.example.sortorder

import android.content.Context
import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics

object AnalyticsTracker {

    private var analytics: FirebaseAnalytics? = null

    fun initialize(context: Context) {
        if (analytics != null || !hasFirebaseConfig(context)) return
        analytics = FirebaseAnalytics.getInstance(context.applicationContext)
    }

    fun isEnabled(): Boolean = analytics != null

    fun logScreen(screenName: String, screenClass: String) {
        logEvent(
            FirebaseAnalytics.Event.SCREEN_VIEW,
            Bundle().apply {
                putString(FirebaseAnalytics.Param.SCREEN_NAME, screenName)
                putString(FirebaseAnalytics.Param.SCREEN_CLASS, screenClass)
            }
        )
    }

    fun logScreenShow(screenName: String) {
        logEvent("${screenName}_show")
    }

    fun logButton(screenName: String, buttonName: String) {
        logEvent("${screenName}_${buttonName}")
    }

    fun logAppSessionStart(sessionNumber: Int, isAdFree: Boolean) {
        logEvent(
            "app_session_start",
            Bundle().apply {
                putIntParam("session_number", sessionNumber)
                putFlag("is_ad_free", isAdFree)
            }
        )
    }

    fun logPlayTap(hasSavedGame: Boolean) {
        logEvent(
            "play_tap",
            Bundle().apply {
                putFlag("has_saved_game", hasSavedGame)
            }
        )
    }

    fun logLevelStart(level: Int, digits: Int, usesAdjacentRule: Boolean, isRestored: Boolean) {
        logEvent(
            "level_start",
            Bundle().apply {
                putLevelParams(level)
                putIntParam("digits", digits)
                putFlag("adjacent_rule", usesAdjacentRule)
                putFlag("is_restored", isRestored)
            }
        )
    }

    fun logGameLevelReached(level: Int) {
        logEvent(
            "game_lv",
            Bundle().apply {
                putLevelParams(level)
            }
        )
    }

    fun logGameSwap(level: Int) {
        logEvent(
            "game_swap",
            Bundle().apply {
                putLevelParams(level)
            }
        )
    }

    fun logOrderCheck(level: Int, correctCount: Int, totalDigits: Int, checksUsed: Int) {
        logEvent(
            "order_check",
            Bundle().apply {
                putLevelParams(level)
                putIntParam("correct_count", correctCount)
                putIntParam("total_digits", totalDigits)
                putIntParam("checks_used", checksUsed)
            }
        )
    }

    fun logHintUse(level: Int, hintsUsed: Int, hintsRemaining: Int) {
        logEvent(
            "hint_use",
            Bundle().apply {
                putLevelParams(level)
                putIntParam("hints_used", hintsUsed)
                putIntParam("hints_remaining", hintsRemaining)
            }
        )
    }

    fun logLevelComplete(
        level: Int,
        score: Int,
        levelScore: Int,
        stars: Int,
        timeLeft: Int,
        checksUsed: Int,
        hintsUsed: Int,
        combo: Int
    ) {
        logEvent(
            "level_complete",
            Bundle().apply {
                putLevelParams(level)
                putIntParam("score", score)
                putIntParam("level_score", levelScore)
                putIntParam("stars", stars)
                putIntParam("time_left", timeLeft)
                putIntParam("checks_used", checksUsed)
                putIntParam("hints_used", hintsUsed)
                putIntParam("combo", combo)
            }
        )
    }

    fun logGameOver(level: Int, score: Int, checksUsed: Int, hintsUsed: Int) {
        logEvent(
            "game_over",
            Bundle().apply {
                putLevelParams(level)
                putIntParam("score", score)
                putIntParam("checks_used", checksUsed)
                putIntParam("hints_used", hintsUsed)
            }
        )
    }

    fun logExtraTimeRequest(method: String, level: Int, coinBalance: Int) {
        logEvent(
            "extra_time_request",
            Bundle().apply {
                putString("method", method)
                putLevelParams(level)
                putIntParam("coin_balance", coinBalance)
            }
        )
    }

    fun logExtraTimeResult(method: String, level: Int, granted: Boolean, seconds: Int) {
        logEvent(
            "extra_time_result",
            Bundle().apply {
                putString("method", method)
                putLevelParams(level)
                putFlag("granted", granted)
                putIntParam("seconds", seconds)
            }
        )
    }

    fun logPremiumPurchaseStart() {
        logEvent("premium_purchase_start")
    }

    fun logPremiumPurchaseSuccess(productId: String, wasAlreadyAdFree: Boolean) {
        logEvent(
            "premium_purchase_success",
            Bundle().apply {
                putString("product_id", productId)
                putFlag("was_already_ad_free", wasAlreadyAdFree)
            }
        )
    }

    fun logSettingsSoundToggle(isEnabled: Boolean) {
        logEvent(
            "settings_sound_toggle",
            Bundle().apply {
                putFlag("is_enabled", isEnabled)
            }
        )
    }

    fun logNavigation(action: String) {
        logEvent(
            "navigation_tap",
            Bundle().apply {
                putString("action", action)
            }
        )
    }

    private fun logEvent(name: String, params: Bundle? = null) {
        analytics?.logEvent(name, params)
    }

    private fun hasFirebaseConfig(context: Context): Boolean {
        val resourceId = context.resources.getIdentifier(
            "google_app_id",
            "string",
            context.packageName
        )
        return resourceId != 0 && context.getString(resourceId).isNotBlank()
    }

    private fun Bundle.putIntParam(key: String, value: Int) {
        putLong(key, value.toLong())
    }

    private fun Bundle.putLevelParams(level: Int) {
        putIntParam("level", level)
        putString("level_name", "lv$level")
    }

    private fun Bundle.putFlag(key: String, value: Boolean) {
        putLong(key, if (value) 1L else 0L)
    }
}
