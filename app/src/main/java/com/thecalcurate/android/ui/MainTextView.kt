package com.thecalcurate.android.ui

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import java.text.DecimalFormat
import java.text.NumberFormat
import java.util.*

class MainTextView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : androidx.appcompat.widget.AppCompatTextView(context, attrs, defStyle) {
    val TAG = "MainTextView"
    fun setResult(result: Double) {
        text = result.toString()
        Log.e(TAG, "setResult result: $result, text: $text")
        /*if (!ifReallyDecimal(result)) {
        val roundedResult = Math.round(result * 1000) / 1000.0
        doubleToStringNoDecimal2(roundedResult).toString()
    } else {
        doubleToStringNoDecimal(result).toString()
    }*/
    }

    fun getResult(): Double {
        var txt = text.replace(Regex(","), "")
        return txt.toDouble()
    }

    // Whether value if a double or not
    private fun ifReallyDecimal(result: Double): Boolean {
        return result == result.toInt().toDouble()
    }

    fun swipe() {
        var result = text.toString().replace(",", "")
        if (result.length == 1) {
            setResult(.0)
        } else {
            var r = result.substring(0, result.length - 1).toDouble()
            setResult(r)
        }
    }

    override fun setText(text: CharSequence?, type: BufferType?) {
        var textStr = text.toString()
        Log.e(TAG, "setText textStr: $textStr")

        var newText = if (textStr.isNotEmpty() && textStr[textStr.length - 1] == '.') {
            textStr
        } else {
            var result = text.toString().toDouble()
            Log.e(TAG, "setText result: $result")

            if (!ifReallyDecimal(result)) {
                val roundedResult = Math.round(result * 1000) / 1000.0
                doubleToStringNoDecimal2(roundedResult).toString()
            } else {
                doubleToStringNoDecimal(result).toString()
            }
        }
        Log.e(TAG, "setText newText: $newText")

        super.setText(newText, type)
    }

    override fun getText(): CharSequence {
        var txt = super.getText()
        return txt.replace(Regex(","), "")
    }

    fun doubleToStringNoDecimal(d: Double): String? {
        val formatter: DecimalFormat = NumberFormat.getInstance(Locale.US) as DecimalFormat
        formatter.applyPattern("#,###")
        return formatter.format(d)
    }

    fun doubleToStringNoDecimal2(d: Double): String? {
        val formatter: DecimalFormat = NumberFormat.getInstance(Locale.US) as DecimalFormat
        formatter.applyPattern("#,###.###")
        return formatter.format(d)
    }
}