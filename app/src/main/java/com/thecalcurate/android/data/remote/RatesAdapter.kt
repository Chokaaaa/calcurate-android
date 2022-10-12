package com.thecalcurate.android.data.remote

import android.util.Log
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.thecalcurate.android.model.Rate
import java.lang.reflect.Type


class RatesAdapter : JsonDeserializer<MutableList<Rate>> {
    override fun deserialize(
        json: JsonElement,
        typeOfT: Type?,
        ctx: JsonDeserializationContext
    ): MutableList<Rate> {
//        Log.e("RatesAdapter", "deserialize json: $json")
        val rates: MutableList<Rate> = ArrayList()

        if (json.isJsonObject) {
            for ((code, value) in json.asJsonObject.entrySet()) {
                rates.add(Rate(code, value.asDouble))
            }
        }
        return rates
    }
}