package com.thecalcurate.android

import android.os.Bundle
import android.util.TypedValue
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.lang.Exception

class MainActivity : AppCompatActivity() {
    lateinit var b0: Button
    lateinit var b1: Button
    lateinit var b2: Button
    lateinit var b3: Button
    lateinit var b4: Button
    lateinit var b5: Button
    lateinit var b6: Button
    lateinit var b7: Button
    lateinit var b8: Button
    lateinit var b9: Button
    lateinit var b_equal: Button
    lateinit var b_multi: Button
    lateinit var b_divide: Button
    lateinit var b_add: Button
    lateinit var b_sub: Button
    lateinit var b_percent: Button
    lateinit var b_clear: Button

    //    private var b_dot: Button? = null
    //    private var b_para2: Button? = null
    lateinit var txvResult: TextView

    private val ADDITION = '+'
    private val SUBTRACTION = '-'
    private val MULTIPLICATION = '*'
    private val DIVISION = '/'
    private val EQU = '='

    //    private val EXTRA = '@'
    private val PERCENT = '%'
    private var SELECTED_ACTION = ' '
    private var isActionSelected = false
    private var val1 = Double.NaN
    private var val2 = Double.NaN

    private val onNumberClickListener = View.OnClickListener {
        val number = getNumberClicked(it.id)
        ifErrorOnOutput()
//        exceedLength()

        if (isActionSelected || txvResult.text.toString() == "0") {
            txvResult.text = number.toString()
            isActionSelected = false
        } else {
            txvResult.text = txvResult.text.toString() + number
        }
    }

    private fun getNumberClicked(viewId: Int): Int {
        return when (viewId) {
            R.id.btn0 -> 0
            R.id.btn1 -> 1
            R.id.btn2 -> 2
            R.id.btn3 -> 3
            R.id.btn4 -> 4
            R.id.btn5 -> 5
            R.id.btn6 -> 6
            R.id.btn7 -> 7
            R.id.btn8 -> 8
            R.id.btn9 -> 9
            else -> 0
        }
    }


    private fun viewSetup() {
        b0 = findViewById(R.id.btn0)
        b1 = findViewById(R.id.btn1)
        b2 = findViewById(R.id.btn2)
        b3 = findViewById(R.id.btn3)
        b4 = findViewById(R.id.btn4)
        b5 = findViewById(R.id.btn5)
        b6 = findViewById(R.id.btn6)
        b7 = findViewById(R.id.btn7)
        b8 = findViewById(R.id.btn8)
        b9 = findViewById(R.id.btn9)
        b_equal = findViewById(R.id.btn_equal)
        b_multi = findViewById(R.id.btn_multi)
        b_divide = findViewById(R.id.btn_divide)
        b_add = findViewById(R.id.btn_add)
        b_sub = findViewById(R.id.btn_sub)
        b_clear = findViewById(R.id.btn_clear)
//        b_dot = findViewById(R.id.btn_dot)
        b_percent = findViewById(R.id.btn_percent)
//        b_para2 = findViewById(R.id.btn_para2)
        txvResult = findViewById(R.id.txtResult)
//        t2 = findViewById(R.id.output)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        viewSetup()

        b0.setOnClickListener(onNumberClickListener)
        b1.setOnClickListener(onNumberClickListener)
        b2.setOnClickListener(onNumberClickListener)
        b3.setOnClickListener(onNumberClickListener)
        b4.setOnClickListener(onNumberClickListener)
        b5.setOnClickListener(onNumberClickListener)
        b6.setOnClickListener(onNumberClickListener)
        b7.setOnClickListener(onNumberClickListener)
        b8.setOnClickListener(onNumberClickListener)
        b9.setOnClickListener(onNumberClickListener)

//        b_dot.setOnClickListener {
//            exceedLength()
//            txvResult.text = txvResult.text.toString() + "."
//        }

        b_percent.setOnClickListener {
            if (txvResult.text.isNotEmpty()) {
                operation(PERCENT)
                isActionSelected = true
//                txvResult.setText(null)
            }
        }
        b_add.setOnClickListener {
            if (txvResult.text.isNotEmpty()) {
                operation(ADDITION)
                isActionSelected = true
//                txvResult.text = null
            }
        }
        b_sub.setOnClickListener {
            if (txvResult.text.isNotEmpty()) {
                operation(SUBTRACTION)
                isActionSelected = true
//                txvResult.text = null
            }
        }
        b_multi.setOnClickListener {
            if (txvResult.text.isNotEmpty()) {
                operation(MULTIPLICATION)
                isActionSelected = true
//                txvResult.text = null
            }
        }
        b_divide.setOnClickListener {
            if (txvResult.text.isNotEmpty()) {
                operation(DIVISION)
                isActionSelected = true
//                txvResult.text = null
            }
        }
        b_equal.setOnClickListener {
            if (txvResult.text.isNotEmpty()) {
                if (!val1.isNaN() && SELECTED_ACTION != ' ') {
                    val2 = txvResult.text.toString().toDouble()
                    val result = calculate(val1, val2, SELECTED_ACTION)
                    showResult(result)
                    val1 = result
                }
            }
        }
        b_clear.setOnClickListener {
            val1 = Double.NaN
            val2 = Double.NaN
            SELECTED_ACTION = ' '
            txvResult.text = "0"
        }

//        b_para2.setOnClickListener {
//            if (!t2.isEmpty() || !txvResult.text.toString().isEmpty()) {
//                val1 = txvResult.text.toString().toDouble()
//                ACTION = EXTRA
//                t2 = "-" + txvResult.text.toString()
//                txvResult.text = ""
//            } else {
//                t2 = "Error"
//            }
//        }

    }

    private fun showResult(result: Double) {
        txvResult.text = if (!ifReallyDecimal(result)) {
            result.toString()
        } else {
            result.toInt().toString()
        }
    }

    private fun calculate(val1: Double, val2: Double, operation: Char): Double {
        return when (operation) {
            ADDITION -> val1 + val2
            SUBTRACTION -> val1 - val2
            MULTIPLICATION -> val1 * val2
            DIVISION -> val1 / val2
//                    EXTRA -> val1 *= -1
            //todo percent
//                PERCENT -> val1 %= val2
            else -> .0
        }
    }

    private fun operation(ACTION: Char) {
        try {
            if (!isActionSelected) {
                if (!val1.isNaN()) {
                    val2 = txvResult.text.toString().toDouble()
                    val result = calculate(val1, val2, SELECTED_ACTION)
                    showResult(result)
                    val1 = result
                } else {
                    val1 = txvResult.text.toString().toDouble()
                }
            }
            SELECTED_ACTION = ACTION
        } catch (e: Exception) {
            txvResult.text = "Error"
        }
    }

    // Remove error message that is already written there.
    private fun ifErrorOnOutput() {
        if (txvResult.text == "Error") {
            val1 = .0
            val2 = .0
            txvResult.text = "0"
        }
    }

    // Whether value if a double or not
    private fun ifReallyDecimal(result: Double): Boolean {
        return result == result.toInt().toDouble()
    }

}