package com.thecalcurate.android.model

data class Currency(
    val name: String,
    val code: String,
    val countryCode: String,
    val isFavorite: Boolean,
    val rateOnBase: Double
)