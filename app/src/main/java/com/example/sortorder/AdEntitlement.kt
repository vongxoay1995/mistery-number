package com.example.sortorder

import android.content.Context

class AdEntitlement(context: Context) {

    private val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    fun isAdFree(): Boolean = prefs.getBoolean(KEY_AD_FREE, false)

    fun unlockAdFree() {
        prefs.edit().putBoolean(KEY_AD_FREE, true).apply()
    }

    companion object {
        private const val PREF_NAME = "purchase_entitlements"
        private const val KEY_AD_FREE = "ad_free"
    }
}
