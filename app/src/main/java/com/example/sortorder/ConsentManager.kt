package com.example.sortorder

import android.app.Activity
import android.content.Context
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform

object ConsentManager {

    private var consentInformation: ConsentInformation? = null

    fun gatherConsent(activity: Activity, onComplete: () -> Unit) {
        val consentInformation = getConsentInformation(activity)
        val params = ConsentRequestParameters.Builder().build()

        consentInformation.requestConsentInfoUpdate(
            activity,
            params,
            {
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) {
                    onComplete()
                }
            },
            {
                onComplete()
            }
        )
    }

    fun canRequestAds(context: Context): Boolean {
        return getConsentInformation(context).canRequestAds()
    }

    fun isPrivacyOptionsRequired(context: Context): Boolean {
        return getConsentInformation(context).privacyOptionsRequirementStatus ==
            ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED
    }

    fun showPrivacyOptionsForm(activity: Activity, onComplete: () -> Unit = {}) {
        UserMessagingPlatform.showPrivacyOptionsForm(activity) {
            onComplete()
        }
    }

    private fun getConsentInformation(context: Context): ConsentInformation {
        val existing = consentInformation
        if (existing != null) return existing

        return UserMessagingPlatform
            .getConsentInformation(context.applicationContext)
            .also { consentInformation = it }
    }
}
