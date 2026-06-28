package com.example.sortorder

import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams

class PremiumActivity : AppCompatActivity() {

    private lateinit var billingClient: BillingClient
    private lateinit var adEntitlement: AdEntitlement
    private lateinit var btnContinue: TextView
    private lateinit var tvPurchaseCaption: TextView
    private lateinit var tvBillingStatus: TextView

    private var removeAdsDetails: ProductDetails? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_premium)
        FullScreenHelper.apply(this)

        adEntitlement = AdEntitlement(this)
        btnContinue = findViewById(R.id.btnContinue)
        tvPurchaseCaption = findViewById(R.id.tvPurchaseCaption)
        tvBillingStatus = findViewById(R.id.tvBillingStatus)

        findViewById<View>(R.id.btnClose).setOnClickListener { finish() }
        btnContinue.setOnClickListener { launchRemoveAdsPurchase() }

        updateUi()
        setupBilling()
    }

    private fun setupBilling() {
        billingClient = BillingClient.newBuilder(this)
            .setListener { billingResult, purchases ->
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    purchases.orEmpty().forEach(::handlePurchase)
                } else if (billingResult.responseCode !=
                    BillingClient.BillingResponseCode.USER_CANCELED
                ) {
                    showBillingStatus(getString(R.string.billing_error))
                }
            }
            .enablePendingPurchases(
                PendingPurchasesParams.newBuilder()
                    .enableOneTimeProducts()
                    .build()
            )
            .build()

        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    queryProduct()
                    restorePurchases()
                } else {
                    showBillingStatus(getString(R.string.billing_unavailable))
                }
            }

            override fun onBillingServiceDisconnected() {
                btnContinue.isEnabled = false
                btnContinue.alpha = DISABLED_ALPHA
                showBillingStatus(getString(R.string.billing_disconnected))
            }
        })
    }

    private fun queryProduct() {
        val product = QueryProductDetailsParams.Product.newBuilder()
            .setProductId(PRODUCT_REMOVE_ADS)
            .setProductType(BillingClient.ProductType.INAPP)
            .build()
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(listOf(product))
            .build()

        billingClient.queryProductDetailsAsync(params) { billingResult, result ->
            removeAdsDetails = result.productDetailsList.firstOrNull()
            updateUi()
            if (billingResult.responseCode != BillingClient.BillingResponseCode.OK ||
                removeAdsDetails == null
            ) {
                showBillingStatus(getString(R.string.billing_products_unavailable))
            }
        }
    }

    private fun restorePurchases() {
        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.INAPP)
            .build()
        billingClient.queryPurchasesAsync(params) { billingResult, purchases ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                purchases.forEach(::handlePurchase)
            }
        }
    }

    private fun launchRemoveAdsPurchase() {
        if (adEntitlement.isAdFree()) return
        val details = removeAdsDetails
        if (details == null) {
            Toast.makeText(this, R.string.billing_unavailable, Toast.LENGTH_SHORT).show()
            return
        }

        val productParamsBuilder = BillingFlowParams.ProductDetailsParams.newBuilder()
            .setProductDetails(details)
        details.oneTimePurchaseOfferDetailsList?.firstOrNull()?.offerToken?.let {
            productParamsBuilder.setOfferToken(it)
        }
        val flowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(listOf(productParamsBuilder.build()))
            .build()
        val result = billingClient.launchBillingFlow(this, flowParams)
        if (result.responseCode != BillingClient.BillingResponseCode.OK) {
            showBillingStatus(getString(R.string.billing_error))
        }
    }

    private fun handlePurchase(purchase: Purchase) {
        if (purchase.purchaseState != Purchase.PurchaseState.PURCHASED) {
            showBillingStatus(getString(R.string.purchase_pending))
            return
        }

        if (PRODUCT_REMOVE_ADS in purchase.products) {
            grantAdFree(purchase)
        }
    }

    private fun grantAdFree(purchase: Purchase) {
        val wasAdFree = adEntitlement.isAdFree()
        adEntitlement.unlockAdFree()
        updateUi()
        if (!wasAdFree) {
            Toast.makeText(this, R.string.ads_removed_success, Toast.LENGTH_SHORT).show()
        }

        if (!purchase.isAcknowledged) {
            val params = AcknowledgePurchaseParams.newBuilder()
                .setPurchaseToken(purchase.purchaseToken)
                .build()
            billingClient.acknowledgePurchase(params) { }
        }
    }

    private fun updateUi() {
        val price = removeAdsDetails
            ?.oneTimePurchaseOfferDetailsList
            ?.firstOrNull()
            ?.formattedPrice
            ?: getString(R.string.premium_price_fallback)
        tvPurchaseCaption.text = getString(R.string.premium_purchase_caption, price)

        if (adEntitlement.isAdFree()) {
            btnContinue.setText(R.string.premium_already_active)
            btnContinue.isEnabled = false
            btnContinue.alpha = DISABLED_ALPHA
            tvBillingStatus.visibility = View.GONE
            return
        }

        btnContinue.setText(R.string.premium_continue)
        btnContinue.isEnabled = removeAdsDetails != null
        btnContinue.alpha = if (btnContinue.isEnabled) 1f else DISABLED_ALPHA
    }

    private fun showBillingStatus(message: String) {
        tvBillingStatus.text = message
        tvBillingStatus.visibility = View.VISIBLE
    }

    override fun onResume() {
        super.onResume()
        if (::adEntitlement.isInitialized) updateUi()
    }

    override fun onDestroy() {
        if (::billingClient.isInitialized) billingClient.endConnection()
        super.onDestroy()
    }

    companion object {
        private const val PRODUCT_REMOVE_ADS = "remove_ads"
        private const val DISABLED_ALPHA = 0.55f
    }
}
