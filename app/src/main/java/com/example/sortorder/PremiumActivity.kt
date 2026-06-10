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
import com.android.billingclient.api.ConsumeParams
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams

class PremiumActivity : AppCompatActivity() {

    private lateinit var billingClient: BillingClient
    private lateinit var coinWallet: CoinWallet
    private lateinit var adEntitlement: AdEntitlement
    private lateinit var tvCoinBalance: TextView
    private lateinit var tvBillingStatus: TextView
    private lateinit var btnBuy100: TextView
    private lateinit var btnRemoveAds: TextView

    private val productDetails = mutableMapOf<String, ProductDetails>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_premium)
        FullScreenHelper.apply(this)

        coinWallet = CoinWallet(this)
        adEntitlement = AdEntitlement(this)
        tvCoinBalance = findViewById(R.id.tvCoinBalance)
        tvBillingStatus = findViewById(R.id.tvBillingStatus)
        btnBuy100 = findViewById(R.id.btnBuy100)
        btnRemoveAds = findViewById(R.id.btnRemoveAds)

        findViewById<View>(R.id.btnBack).setOnClickListener { finish() }
        btnBuy100.setOnClickListener { launchPurchase(PRODUCT_COIN_100) }
        btnRemoveAds.setOnClickListener { launchPurchase(PRODUCT_REMOVE_ADS) }

        updateUi()
        setPurchaseButtonsEnabled(false)
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
                    queryProducts()
                    restorePurchases()
                } else {
                    showBillingStatus(getString(R.string.billing_unavailable))
                }
            }

            override fun onBillingServiceDisconnected() {
                setPurchaseButtonsEnabled(false)
                showBillingStatus(getString(R.string.billing_disconnected))
            }
        })
    }

    private fun queryProducts() {
        val products = listOf(PRODUCT_COIN_100, PRODUCT_REMOVE_ADS).map { productId ->
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(productId)
                .setProductType(BillingClient.ProductType.INAPP)
                .build()
        }
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(products)
            .build()

        billingClient.queryProductDetailsAsync(params) { billingResult, result ->
            productDetails.clear()
            result.productDetailsList.forEach { details ->
                productDetails[details.productId] = details
            }
            val ready = billingResult.responseCode == BillingClient.BillingResponseCode.OK &&
                productDetails.keys.containsAll(listOf(PRODUCT_COIN_100, PRODUCT_REMOVE_ADS))
            updatePurchaseLabels()
            showBillingStatus(
                if (ready) {
                    getString(R.string.billing_ready)
                } else {
                    getString(R.string.billing_products_unavailable)
                }
            )
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

    private fun launchPurchase(productId: String) {
        if (productId == PRODUCT_REMOVE_ADS && adEntitlement.isAdFree()) return
        val details = productDetails[productId]
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

        when {
            PRODUCT_COIN_100 in purchase.products -> grantCoins(purchase)
            PRODUCT_REMOVE_ADS in purchase.products -> grantAdFree(purchase)
        }
    }

    private fun grantCoins(purchase: Purchase) {
        val granted = coinWallet.grantPurchase(purchase.purchaseToken, COIN_PACK_AMOUNT)
        if (granted) {
            updateUi()
            Toast.makeText(
                this,
                getString(R.string.coins_added, COIN_PACK_AMOUNT),
                Toast.LENGTH_SHORT
            ).show()
        }

        val consumeParams = ConsumeParams.newBuilder()
            .setPurchaseToken(purchase.purchaseToken)
            .build()
        billingClient.consumeAsync(consumeParams) { _, _ -> }
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
        tvCoinBalance.text = getString(R.string.coin_balance, coinWallet.getBalance())
        updatePurchaseLabels()
    }

    private fun updatePurchaseLabels() {
        btnBuy100.text = productLabel(PRODUCT_COIN_100, R.string.coin_pack_100)
        btnBuy100.isEnabled = productDetails.containsKey(PRODUCT_COIN_100)
        btnBuy100.alpha = if (btnBuy100.isEnabled) 1f else 0.5f
        if (adEntitlement.isAdFree()) {
            btnRemoveAds.setText(R.string.ads_already_removed)
            btnRemoveAds.isEnabled = false
            btnRemoveAds.alpha = 0.55f
        } else {
            btnRemoveAds.text = productLabel(PRODUCT_REMOVE_ADS, R.string.remove_all_ads)
            btnRemoveAds.isEnabled = productDetails.containsKey(PRODUCT_REMOVE_ADS)
            btnRemoveAds.alpha = if (btnRemoveAds.isEnabled) 1f else 0.5f
        }
    }

    private fun productLabel(productId: String, fallbackLabel: Int): String {
        val price = productDetails[productId]
            ?.oneTimePurchaseOfferDetailsList
            ?.firstOrNull()
            ?.formattedPrice
        return if (price.isNullOrBlank()) {
            getString(fallbackLabel)
        } else {
            getString(R.string.iap_product_with_price, getString(fallbackLabel), price)
        }
    }

    private fun setPurchaseButtonsEnabled(enabled: Boolean) {
        btnBuy100.isEnabled = enabled
        btnBuy100.alpha = if (enabled) 1f else 0.5f
        if (!adEntitlement.isAdFree()) {
            btnRemoveAds.isEnabled = enabled
            btnRemoveAds.alpha = if (enabled) 1f else 0.5f
        }
    }

    private fun showBillingStatus(message: String) {
        tvBillingStatus.text = message
    }

    override fun onResume() {
        super.onResume()
        if (::coinWallet.isInitialized) updateUi()
    }

    override fun onDestroy() {
        if (::billingClient.isInitialized) billingClient.endConnection()
        super.onDestroy()
    }

    companion object {
        private const val PRODUCT_COIN_100 = "coin_100"
        private const val PRODUCT_REMOVE_ADS = "remove_all_ads"
        private const val COIN_PACK_AMOUNT = 100
    }
}
