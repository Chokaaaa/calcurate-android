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
        var resText = result.toString()
        if (resText.length > 1 && resText[resText.length - 2] == '.' && resText[resText.length - 1] == '0') {
            resText = resText.substring(0, resText.indexOf(".0"))
        }
        text = resText
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
        text = if (result.length == 1) {
            "0"
        } else {
            result.substring(0, result.length - 1)
        }
    }

    override fun setText(text: CharSequence?, type: BufferType?) {
        var textStr = text.toString()

        var newText = if (textStr.isNotEmpty() && textStr[textStr.length - 1] == '.') {
            var result = textStr.toDouble()
            if (ifReallyDecimal(result)) {
                doubleToStringNoDecimal(result).toString() + "."
            } else {
                textStr
            }
        } else if (textStr.length > 1 && textStr.contains('.')) {
            var beforeDot = textStr.split('.')[0]
            var afterDot = textStr.split('.')[1]
            doubleToStringNoDecimal(beforeDot.toDouble()) + "." + afterDot
//            var result = textStr.toDouble()
//            Log.e(TAG, "setText .0 result: $result")
//            if (ifReallyDecimal(result)) {
//                doubleToStringNoDecimal2(result).toString() + ".0"
//            } else {
//            textStr
//            }
        } else {
            var result = textStr.toDouble()
            Log.e(TAG, "setText else result: $result")

            if (!ifReallyDecimal(result)) {
                val roundedResult = Math.round(result * 1000) / 1000.0
                doubleToStringNoDecimal2(roundedResult).toString()
            } else {
                doubleToStringNoDecimal(result).toString()
            }

        }
        Log.e(TAG, "setText textStr: $textStr, newText: $newText")

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