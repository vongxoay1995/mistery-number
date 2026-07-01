package com.example.sortorder

import android.app.Activity
import android.app.Application
import com.google.android.gms.ads.MobileAds

class SortOrderApplication : Application() {

    private lateinit var appOpenAdManager: AppOpenAdManager
    private var sessionNumber = 0
    private var adsInitialized = false

    override fun onCreate() {
        super.onCreate()
        AnalyticsTracker.initialize(this)
        sessionNumber = incrementSessionNumber()
        AnalyticsTracker.logAppSessionStart(
            sessionNumber = sessionNumber,
            isAdFree = AdEntitlement(this).isAdFree()
        )
    }

    fun initializeAdsIfAllowed() {
        if (adsInitialized || AdEntitlement(this).isAdFree() || !ConsentManager.canRequestAds(this)) {
            return
        }

        adsInitialized = true
        MobileAds.initialize(this) { }
        if (!::appOpenAdManager.isInitialized) {
            appOpenAdManager = AppOpenAdManager(this, AdMobIds.APP_OPEN)
        }
        appOpenAdManager.loadAd()
    }

    fun showAppOpenAdIfAvailable(activity: Activity, onComplete: () -> Unit) {
        initializeAdsIfAllowed()
        if (sessionNumber < FIRST_APP_OPEN_AD_SESSION ||
            AdEntitlement(this).isAdFree() ||
            !ConsentManager.canRequestAds(this) ||
            !::appOpenAdManager.isInitialized
        ) {
            onComplete()
            return
        }
        appOpenAdManager.showAdIfAvailable(activity, onComplete)
    }

    private fun incrementSessionNumber(): Int {
        val prefs = getSharedPreferences(PREF_NAME, MODE_PRIVATE)
        val nextSession = prefs.getInt(KEY_SESSION_NUMBER, 0) + 1
        prefs.edit().putInt(KEY_SESSION_NUMBER, nextSession).apply()
        return nextSession
    }

    companion object {
        private const val PREF_NAME = "ad_session_prefs"
        private const val KEY_SESSION_NUMBER = "session_number"
        private const val FIRST_APP_OPEN_AD_SESSION = 1
    }
}
