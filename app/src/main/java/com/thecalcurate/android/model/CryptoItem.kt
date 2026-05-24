package com.thecalcurate.android.model

import com.thecalcurate.android.R

data class CryptoItem(
    val name: String,
    val code: String,
    val iconResId: Int,
    val rate: Double = .0
) {
    var isFavorite2: Boolean = false

    fun getRateStr(): String =
        if (rate == .0) "0" else "%.3f".format(rate)

    companion object {
        fun getList(): MutableList<CryptoItem> = mutableListOf(
            CryptoItem("Bitcoin",     "BTC",  R.drawable.crypto_btc),
            CryptoItem("Ethereum",    "ETH",  R.drawable.crypto_eth),
            CryptoItem("Tether",      "USDT", R.drawable.crypto_usdt),
            CryptoItem("BNB",         "BNB",  R.drawable.crypto_bnb),
            CryptoItem("Solana",      "SOL",  R.drawable.crypto_sol),
            CryptoItem("XRP",         "XRP",  R.drawable.crypto_xrp),
            CryptoItem("Cardano",     "ADA",  R.drawable.crypto_ada),
            CryptoItem("Dogecoin",    "DOGE", R.drawable.crypto_doge),
        )

        fun codes(): List<String> = listOf("BTC", "ETH", "USDT", "BNB", "SOL", "XRP", "ADA", "DOGE")

        fun isCryptoCode(code: String): Boolean = codes().contains(code)
    }
}
