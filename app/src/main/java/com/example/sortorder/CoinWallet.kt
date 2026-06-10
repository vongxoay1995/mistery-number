package com.example.sortorder

import android.content.Context

class CoinWallet(context: Context) {

    private val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    fun getBalance(): Int {
        ensureInitialized()
        return prefs.getInt(KEY_BALANCE, INITIAL_COINS)
    }

    fun add(amount: Int): Int {
        require(amount >= 0)
        val newBalance = getBalance() + amount
        prefs.edit().putInt(KEY_BALANCE, newBalance).apply()
        return newBalance
    }

    fun spend(amount: Int): Boolean {
        require(amount >= 0)
        val balance = getBalance()
        if (balance < amount) return false
        prefs.edit().putInt(KEY_BALANCE, balance - amount).apply()
        return true
    }

    fun grantPurchase(purchaseToken: String, amount: Int): Boolean {
        require(amount > 0)
        val processedTokens = prefs.getStringSet(KEY_PROCESSED_PURCHASES, emptySet()).orEmpty()
        if (purchaseToken in processedTokens) return false

        val updatedTokens = processedTokens.toMutableSet().apply { add(purchaseToken) }
        prefs.edit()
            .putInt(KEY_BALANCE, getBalance() + amount)
            .putStringSet(KEY_PROCESSED_PURCHASES, updatedTokens)
            .apply()
        return true
    }

    private fun ensureInitialized() {
        if (!prefs.contains(KEY_BALANCE)) {
            prefs.edit().putInt(KEY_BALANCE, INITIAL_COINS).apply()
        }
    }

    companion object {
        const val INITIAL_COINS = 15
        const val EXTRA_TIME_COST = 15

        private const val PREF_NAME = "coin_wallet"
        private const val KEY_BALANCE = "balance"
        private const val KEY_PROCESSED_PURCHASES = "processed_purchases"
    }
}
