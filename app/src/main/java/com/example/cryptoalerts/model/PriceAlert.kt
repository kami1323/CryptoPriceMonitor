package com.example.cryptoalerts.model

data class PriceAlert(
    val id: String = System.currentTimeMillis().toString(), // Unique ID for the alert
    val cryptoSymbol: String,
    val cryptoName: String,
    var currentPrice: Double,
    val alertPrice: Double,
    var priceChangePercent: Double,
    var isTriggered: Boolean = false
) {
    fun isPriceThresholdCrossed(): Boolean {
        // Check if the current price has crossed the alert price
        return if (alertPrice > currentPrice) {
            // Alert for price going up
            currentPrice >= alertPrice
        } else {
            // Alert for price going down
            currentPrice <= alertPrice
        }
    }
}
