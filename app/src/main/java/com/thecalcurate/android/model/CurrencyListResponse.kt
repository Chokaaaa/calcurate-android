package com.thecalcurate.android.model

data class CurrencyListResponse(
    val base_code: String,
    val rates: MutableList<Rate>,
    val time_last_update_unix: Int = 0,
    val time_last_update_utc: String = "",
    val time_next_update_unix: Int = 0,
    val time_next_update_utc: String = ""
)
