package com.example.cryptoalerts.model

data class CryptoCurrency(
    val symbol: String,
    val name: String,
    val price: Double,
    val priceChangePercent: Double
)

// Response models for Binance API
data class BinanceTickerResponse(
    val symbol: String,
    val price: String
)

data class Binance24hTickerResponse(
    val symbol: String,
    val priceChange: String,
    val priceChangePercent: String,
    val lastPrice: String
)

data class BinanceExchangeInfoResponse(
    val symbols: List<SymbolInfo>
)

data class SymbolInfo(
    val symbol: String,
    val baseAsset: String,
    val quoteAsset: String
)
