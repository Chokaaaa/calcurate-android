package com.thecalcurate.android.ui

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.util.TypedValue
import com.thecalcurate.android.R
import java.math.BigDecimal
import java.text.DecimalFormat
import java.text.NumberFormat
import java.util.*

class MainTextView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : androidx.appcompat.widget.AppCompatTextView(context, attrs, defStyle) {
    val TAG = "MainTextView"
    val maxLength = 12
    fun setResult(result: Double) {
        var resText = result.toBigDecimal().toPlainString()
//        if (result != .0)
//            resText = (Math.round(result * 1000) / 1000.0).toBigDecimal().toPlainString()

        if (resText.length > 1 && resText[resText.length - 2] == '.' && resText[resText.length - 1] == '0') {
            resText = resText.substring(0, resText.indexOf(".0"))
        }
        text = resText
        Log.e(TAG, "setResult resText: $resText, text: $text")
//        if (!ifReallyDecimal(result)) {
//            val roundedResult = Math.round(result * 1000) / 1000.0
//            doubleToStringNoDecimal2(roundedResult).toString()
//        } else {
//            doubleToStringNoDecimal(result).toString()
//        }
    }

    fun getResult(): Double {
        var txt = text.replace(Regex(","), "").filter { it.isDigit() || it == '.' }
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

        var newText =
        if (textStr.isNotEmpty() && textStr[textStr.length - 1] == '.') {
            var result = textStr.filter { it.isDigit() }.toDouble()
            Log.e(TAG, "setText ifReallyDecimal(result): ${ifReallyDecimal(result)}, textStr: $textStr")
//            if (ifReallyDecimal(result)) {
                doubleToStringNoDecimal(result.toBigDecimal()).toString() + "."
//            } else {
//                textStr
//            }
        } else if (textStr.length > 1 && textStr.contains('.')) {
            var beforeDot = textStr.split('.')[0].filter { it.isDigit() }
            var afterDot = textStr.split('.')[1]
            doubleToStringNoDecimal(beforeDot.toBigDecimal()) + "." + afterDot
//            var result = textStr.toDouble()
//            Log.e(TAG, "setText .0 result: $result")
//            if (ifReallyDecimal(result)) {
//                doubleToStringNoDecimal2(result).toString() + ".0"
//            } else {
//            textStr
//            }
        } else {
            if (textStr.isNotEmpty())
                textStr = textStr.filter { it.isDigit() }

            if (textStr.length > maxLength) {
                textStr = textStr.take(maxLength)
            }
            var result =
                if (textStr.isNotEmpty())
                    textStr.filter { it.isDigit() }.toDouble()
                else
                    .0

            Log.e(TAG, "setText else result: $result")

//            if (!ifReallyDecimal(result)) {
//                val roundedResult = Math.round(result * 1000) / 1000.0
            doubleToStringNoDecimal2(result.toBigDecimal())
//            } else {
//                doubleToStringNoDecimal(result.toBigDecimal())
//            }

        }
        Log.e(TAG, "setText textStr: $textStr, newText: $newText")

        super.setText(newText, type)
    }

    override fun getText(): CharSequence {
        var txt = super.getText()
        return txt.replace(Regex(","), "")
    }

    fun doubleToStringNoDecimal(d: BigDecimal): String? {
        val formatter: DecimalFormat = DecimalFormat("###,###,###,###")
//        formatter.applyPattern("###,###,###,###")
        formatter.maximumFractionDigits = 340
        return formatter.format(d)
    }

    fun doubleToStringNoDecimal2(d: BigDecimal): String? {
        val formatter: DecimalFormat =
            DecimalFormat("###,###,###,###.###") //NumberFormat.getInstance(Locale.US) as DecimalFormat
//        formatter.applyPattern("###,###,###,###.###")
        formatter.maximumFractionDigits = 340
        return formatter.format(d)
    }
}