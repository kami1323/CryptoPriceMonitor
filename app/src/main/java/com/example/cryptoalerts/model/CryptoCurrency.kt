package com.example.cryptoalerts.model

data class CryptoCurrency(
    val symbol: String,
    val price: Double,
    val priceChangePercent: Double,
    val volume: Double,
    val lastUpdated: Long = System.currentTimeMillis()
)