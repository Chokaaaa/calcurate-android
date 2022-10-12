package com.thecalcurate.android

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.Observer
import com.blongho.country_data.World
import com.thecalcurate.android.model.CurrencyItem
import com.thecalcurate.android.ui.CurrencyDialog
import com.thecalcurate.android.ui.CurrencyRecyclerViewAdapter
import com.thecalcurate.android.ui.CurrencySwipeListener
import com.thecalcurate.android.ui.MainTextView
import com.thecalcurate.android.viewmodel.CurrencyListViewModel
import java.lang.Exception

class MainActivity : AppCompatActivity(), CurrencyDialog.NoticeDialogListener {
    val TAG = "MainActivity"
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

    lateinit var btn_main: ImageView
    lateinit var btn_secondary1: ImageView
    lateinit var btn_secondary2: ImageView

    lateinit var txvResult: MainTextView

    private val ADDITION = '+'
    private val SUBTRACTION = '-'
    private val MULTIPLICATION = '*'
    private val DIVISION = '/'
    private val EQU = '='

    private val PERCENT = '%'
    private var SELECTED_ACTION = ' '
    private var isActionSelected = false
    private var val1 = Double.NaN
    private var val2 = Double.NaN

    private var longClickedId = 0

    var isMainSelected = false
    var savedMainVal = .0
    var secondarySelectedId = 0

    lateinit var viewModel: CurrencyListViewModel

    var itemClickListener = object : CurrencyRecyclerViewAdapter.ItemClickListener {
        override fun onItemClick(view: View?, position: Int) {
            var curItem = view?.tag as CurrencyItem
            if (longClickedId == R.id.btn_main) {
                btn_main.tag = curItem.code
                viewModel.getCurrencyRates(
                    curItem.code,
                    btn_secondary1.tag as String,
                    btn_secondary2.tag as String
                )
                val resourceId = getResource(curItem.code)
                btn_main.setImageResource(resourceId)
            } else if (longClickedId == R.id.btn_secondary1) {
                btn_secondary1.tag = curItem.code
                val resourceId = getResource(curItem.code)
                btn_secondary1.setImageResource(resourceId)
            } else {
                btn_secondary2.tag = curItem.code
                val resourceId = getResource(curItem.code)
                btn_secondary2.setImageResource(resourceId)
            }
        }
    }

    var newFragment = CurrencyDialog(itemClickListener)

    private fun getResource(code: String): Int {
        return when (code) {
            "KZT" -> R.drawable.kzt
            "USD" -> R.drawable.usd
            "AED" -> R.drawable.aed
            "EUR" -> R.drawable.euro
            "BYN" -> R.drawable.byn
            "BTN" -> R.drawable.btn
            "STN" -> R.drawable.stn
            "ERN" -> R.drawable.ern
            "MRU" -> R.drawable.mru
            "WST" -> R.drawable.wst
            "SLE" -> R.drawable.sle
            "ZMW" -> R.drawable.zmw
            "ARS" -> R.drawable.ars
            "AUD" -> R.drawable.aud
            else -> {
                val curList = World.getAllCurrencies().filter { it.code == code }
                if (curList.isNotEmpty()) {
                    World.getFlagOf(curList[0]!!.country)
                } else {
                    R.drawable.aed
                }
            }
        }
    }


    private val onNumberClickListener = View.OnClickListener {
        val number = getNumberClicked(it.id)
        ifErrorOnOutput()

        if (isActionSelected || txvResult.text.toString() == "0") {
            txvResult.text = number.toString()
            isActionSelected = false
        } else {
            txvResult.text = txvResult.text.toString() + number
        }
    }

    private val onActionClickListener = View.OnClickListener {
        if (txvResult.text.isNotEmpty()) {
            operation(getAction(it.id))
            isActionSelected = true
        }
    }

    private val onPercentageClickListener = View.OnClickListener {
        if (txvResult.text.isNotEmpty()) {
            if (!val1.isNaN()) {
                val2 = txvResult.text.toString().toDouble()
                val result = val1 * (0.01 * val2)
                showResult(result)
            } else {
                val1 = .0
            }
            val result = val1 * (0.01 * val2)
            showResult(result)
        }
    }

    private val onEqualClickListener = View.OnClickListener {
        if (txvResult.text.isNotEmpty()) {
            if (!val1.isNaN() && SELECTED_ACTION != ' ') {
                val2 = txvResult.text.toString().toDouble()
                val result = calculate(val1, val2, SELECTED_ACTION)
                showResult(result)
                val1 = result
                isActionSelected = true
            }
        }
    }
    private val onClearClickListener = View.OnClickListener {
        val1 = Double.NaN
        val2 = Double.NaN
        SELECTED_ACTION = ' '
        txvResult.text = "0"
        savedMainVal = .0
        unselectSecondary()
        secondarySelectedId = 0
    }


    private val onMainClickListener = View.OnClickListener {
        it.isSelected = !it.isSelected
        isMainSelected = it.isSelected
        if (!isMainSelected) {
            unselectSecondary()
            secondarySelectedId = 0
        }
    }

    private fun unselectSecondary() {
        if (secondarySelectedId == R.id.btn_secondary1) {
            btn_secondary1.isSelected = false
        } else if (secondarySelectedId == R.id.btn_secondary2) {
            btn_secondary2.isSelected = false
        }
    }

    private val onSecondaryClickListener = View.OnClickListener {
        Log.e(TAG, "onSecondaryClickListener isMainSelected: $isMainSelected, ")
        if (isMainSelected) {
            if (secondarySelectedId == 0) {
                savedMainVal = txvResult.getResult()
            }
            unselectSecondary()
            if (secondarySelectedId != it.id) {
                secondarySelectedId = it.id
                it.isSelected = true
                convertToSec(it.tag.toString())

            } else if (secondarySelectedId == it.id) {
                secondarySelectedId = 0
                convertBack()
            }
        }
    }

    private fun convertToSec(secCode: String) {
//        Log.e(TAG, "convertToSec: $secCode")
        if (viewModel.currencyRateList.value != null) {
            val rate = viewModel.currencyRateList.value!!.filter { it.Code == secCode }[0].Value
            val converted = savedMainVal * rate
//        Log.e(TAG, "savedMainVal: $savedMainVal, rate: $rate, converted: $converted")
            txvResult.setResult(converted)
        }
    }

    private fun convertBack() {
        Log.e(TAG, "convertBack savedMainVal: $savedMainVal")
        txvResult.setResult(savedMainVal)
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

    private fun getAction(viewId: Int): Char {
        return when (viewId) {
            R.id.btn_add -> ADDITION
            R.id.btn_equal -> EQU
            R.id.btn_divide -> DIVISION
            R.id.btn_multi -> MULTIPLICATION
            R.id.btn_sub -> SUBTRACTION
            R.id.btn_percent -> PERCENT
            else -> ' '
        }
    }


    private fun viewSetup() {
        btn_main = findViewById(R.id.btn_main)
        btn_secondary1 = findViewById(R.id.btn_secondary1)
        btn_secondary2 = findViewById(R.id.btn_secondary2)


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
        b_percent = findViewById(R.id.btn_percent)
        txvResult = findViewById(R.id.txtResult)
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        World.init(applicationContext)

        viewSetup()

        setCurrencyBtn()

        viewModel = CurrencyListViewModel(application, DataRepository())

        viewModel.currencyList.observe(this) {
            Log.e("MainAct", "listSize: " + it.size)
//            adapter.setMovies(it)
        }

        viewModel.errorMessage.observe(this) {
            Toast.makeText(this, it, Toast.LENGTH_SHORT).show()
        }

        viewModel.loading.observe(this, Observer {
            if (it) {
//                binding.progressDialog.visibility = View.VISIBLE
            } else {
//                binding.progressDialog.visibility = View.GONE
            }
        })

        btn_main.tag = "USD"
        btn_secondary1.tag = "AED"
        btn_secondary2.tag = "KZT"

        viewModel.getCurrencyRates("USD", "AED", "KZT")

        btn_main.setImageResource(R.drawable.usd)
        btn_secondary1.setImageResource(R.drawable.aed)
        btn_secondary2.setImageResource(R.drawable.kzt)

//        btn_main.setOnLongClickListener(longClickListener)
        btn_secondary1.setOnLongClickListener(longClickListener)
        btn_secondary2.setOnLongClickListener(longClickListener)

        btn_main.setOnClickListener(onMainClickListener)
        btn_secondary1.setOnClickListener(onSecondaryClickListener)
        btn_secondary2.setOnClickListener(onSecondaryClickListener)

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

        b_percent.setOnClickListener(onPercentageClickListener)
        b_add.setOnClickListener(onActionClickListener)
        b_sub.setOnClickListener(onActionClickListener)
        b_multi.setOnClickListener(onActionClickListener)
        b_divide.setOnClickListener(onActionClickListener)

        b_equal.setOnClickListener(onEqualClickListener)
        b_clear.setOnClickListener(onClearClickListener)

    }

    private val longClickListener = View.OnLongClickListener {
        Log.e("MainActivity", "View On Long Click!!!!")
        longClickedId = it.id
        showDialog()
        false
    }

    private fun setCurrencyBtn() {
        btn_main.setOnTouchListener(object : CurrencySwipeListener(this) {
            override fun onSwipeRight(){
                super.onSwipeRight()
                Log.e("MainActivity", "onSwipeRight")

            }

            override fun onSwipeLeft() {
                super.onSwipeLeft()
                Log.e("MainActivity", "onSwipeLeft")

            }

            override fun onSwipeTop(){
                super.onSwipeTop()
                Log.e("MainActivity", "onSwipeTop")

            }

            override fun onSwipeBottom() {
                super.onSwipeBottom()
                Log.e("MainActivity", "onSwipeBottom")

            }

            override fun onLongPress() {
                super.onLongPress()
                Log.e("MainActivity", "View On Long Click!!!!")
                longClickedId = R.id.btn_main
                showDialog()
            }
        })

    }

    private fun showResult(result: Double) {
        txvResult.setResult(result)
    }

    private fun calculate(val1: Double, val2: Double, operation: Char): Double {
        return when (operation) {
            ADDITION -> val1 + val2
            SUBTRACTION -> val1 - val2
            MULTIPLICATION -> val1 * val2
            DIVISION -> val1 / val2
//                    EXTRA -> val1 *= -1
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


    override fun onDialogNegativeClick(dialog: DialogFragment) {

    }

    private fun showDialog() {
        newFragment.setList(viewModel.currencyList)
        newFragment.show(supportFragmentManager, "dialog")
    }
}