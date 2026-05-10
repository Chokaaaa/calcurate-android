package com.thecalcurate.android.model

data class CurrencyListResponse(
    val base_code: String,
    val conversion_rates: MutableList<Rate>,
    val result: String,
    val time_eol_unix: Int,
    val time_last_update_unix: Int,
    val time_last_update_utc: String,
    val time_next_update_unix: Int,
    val time_next_update_utc: String
)