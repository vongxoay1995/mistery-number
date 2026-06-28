package com.example.sortorder

import android.content.Context
import android.content.Intent
import android.net.Uri

private const val PRIVACY_POLICY_URL =
    "https://sites.google.com/view/privacypolicyforswapnumber/trang-ch%E1%BB%A7"

fun Context.openPrivacyPolicy() {
    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(PRIVACY_POLICY_URL)))
}
