package com.example.sortorder

import android.app.Activity
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError

class BannerAdHelper(
    private val activity: Activity,
    private val container: FrameLayout,
    private val adUnitId: String,
    private val adEntitlement: AdEntitlement = AdEntitlement(activity)
) {

    private var adView: AdView? = null

    fun load() {
        if (adEntitlement.isAdFree()) {
            destroy()
            container.visibility = View.GONE
            return
        }

        container.post {
            if (activity.isFinishing || activity.isDestroyed || adView != null) return@post

            val bannerView = AdView(activity).apply {
                this.adUnitId = this@BannerAdHelper.adUnitId
                setAdSize(AdSize.BANNER)
                adListener = object : AdListener() {
                    override fun onAdLoaded() {
                        container.visibility = View.VISIBLE
                    }

                    override fun onAdFailedToLoad(adError: LoadAdError) {
                        this@apply.destroy()
                        adView = null
                        container.removeAllViews()
                        container.visibility = View.GONE
                    }
                }
            }

            adView = bannerView
            container.removeAllViews()
            container.visibility = View.INVISIBLE
            container.addView(
                bannerView,
                FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    Gravity.CENTER
                )
            )
            bannerView.loadAd(AdRequest.Builder().build())
        }
    }

    fun refreshVisibility() {
        if (adEntitlement.isAdFree()) {
            destroy()
            container.visibility = View.GONE
        } else if (adView == null) {
            load()
        }
    }

    fun resume() {
        adView?.resume()
    }

    fun pause() {
        adView?.pause()
    }

    fun destroy() {
        adView?.destroy()
        adView = null
        container.removeAllViews()
    }

}
