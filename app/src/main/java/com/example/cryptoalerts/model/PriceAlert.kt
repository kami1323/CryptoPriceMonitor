package com.example.cryptoalerts.model

import java.util.UUID

data class PriceAlert(
    val id: String = UUID.randomUUID().toString(),
    val symbol: String,
    val targetPrice: Double,
    val isAboveTarget: Boolean,  // true if alert is for price going above target, false for below
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)