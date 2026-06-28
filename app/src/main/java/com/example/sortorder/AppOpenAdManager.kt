package com.example.sortorder

import android.app.Activity
import android.content.Context
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.appopen.AppOpenAd

class AppOpenAdManager(
    private val context: Context,
    private val adUnitId: String
) {

    private var appOpenAd: AppOpenAd? = null
    private var isLoadingAd = false
    private var isShowingAd = false
    private var loadTime = 0L

    fun loadAd() {
        if (isLoadingAd || isAdAvailable()) return

        isLoadingAd = true
        AppOpenAd.load(
            context,
            adUnitId,
            AdRequest.Builder().build(),
            object : AppOpenAd.AppOpenAdLoadCallback() {
                override fun onAdLoaded(ad: AppOpenAd) {
                    appOpenAd = ad
                    isLoadingAd = false
                    loadTime = System.currentTimeMillis()
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    isLoadingAd = false
                }
            }
        )
    }

    fun showAdIfAvailable(activity: Activity, onComplete: () -> Unit) {
        if (isShowingAd || activity.isFinishing || activity.isDestroyed) {
            onComplete()
            return
        }

        val ad = appOpenAd
        if (ad == null || !isAdAvailable()) {
            loadAd()
            onComplete()
            return
        }

        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                appOpenAd = null
                isShowingAd = false
                onComplete()
                loadAd()
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                appOpenAd = null
                isShowingAd = false
                onComplete()
                loadAd()
            }

            override fun onAdShowedFullScreenContent() {
                isShowingAd = true
            }
        }

        ad.show(activity)
    }

    private fun isAdAvailable(): Boolean {
        return appOpenAd != null && System.currentTimeMillis() - loadTime < AD_EXPIRATION_MS
    }

    companion object {
        private const val AD_EXPIRATION_MS = 4 * 60 * 60 * 1000L
    }
}
