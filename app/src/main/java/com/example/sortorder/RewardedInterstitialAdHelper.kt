package com.example.sortorder

import android.app.Activity
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAd
import com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAdLoadCallback

class RewardedInterstitialAdHelper(
    private val activity: Activity,
    private val adUnitId: String
) {

    private var rewardedInterstitialAd: RewardedInterstitialAd? = null
    private var isLoading = false

    fun load() {
        if (isLoading || rewardedInterstitialAd != null || activity.isFinishing || activity.isDestroyed) {
            return
        }

        isLoading = true
        RewardedInterstitialAd.load(
            activity,
            adUnitId,
            AdRequest.Builder().build(),
            object : RewardedInterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedInterstitialAd) {
                    rewardedInterstitialAd = ad
                    isLoading = false
                }

                override fun onAdFailedToLoad(adError: LoadAdError) {
                    rewardedInterstitialAd = null
                    isLoading = false
                }
            }
        )
    }

    fun show(
        onRewardEarned: () -> Unit,
        onAdUnavailable: () -> Unit,
        onAdClosed: (rewardEarned: Boolean) -> Unit
    ) {
        val ad = rewardedInterstitialAd
        if (ad == null) {
            load()
            onAdUnavailable()
            return
        }

        var rewardEarned = false
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                rewardedInterstitialAd = null
                load()
                onAdClosed(rewardEarned)
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                rewardedInterstitialAd = null
                load()
                onAdUnavailable()
            }
        }

        rewardedInterstitialAd = null
        ad.show(activity) {
            rewardEarned = true
            onRewardEarned()
        }
    }
}
