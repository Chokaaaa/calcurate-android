package com.thecalcurate.android

import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.Observer
import com.blongho.country_data.World
import com.thecalcurate.android.model.CurrencyItem
import com.thecalcurate.android.ui.CurrencyDialog
import com.thecalcurate.android.ui.CurrencyRecyclerViewAdapter
import com.thecalcurate.android.ui.OnSwipeListener
import com.thecalcurate.android.ui.MainTextView
import com.thecalcurate.android.viewmodel.CurrencyListViewModel


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
    lateinit var favList: MutableList<String>
    var mp: MediaPlayer? = null
    val UP = 1
    val DOWN = 2

    var itemClickListener = object : CurrencyRecyclerViewAdapter.ItemClickListener {
        override fun onItemClick(view: View?, position: Int) {
            var curItem = view?.tag as CurrencyItem
            when (longClickedId) {
                R.id.btn_main -> setCur(btn_main, curItem.code)
                R.id.btn_secondary1 -> setCur(btn_secondary1, curItem.code)
                R.id.btn_secondary2 -> setCur(btn_secondary2, curItem.code)
            }
        }
    }

    private fun fetchCurrencyRates(type: Int) {
        val mainCurrencyCode = when (type) {
            viewModel.MAIN -> btn_main.getTag(R.id.code_tag_name).toString()
            viewModel.SEC1 -> btn_secondary1.getTag(R.id.code_tag_name).toString()
            viewModel.SEC2 -> btn_secondary2.getTag(R.id.code_tag_name).toString()
            else -> btn_main.getTag(R.id.code_tag_name).toString()
        }
        viewModel.getCurrencyRates(mainCurrencyCode, type)
    }

    fun setCur(imageView: ImageView, code: String) {
        val resourceId = getResource(code)
        imageView.setImageResource(resourceId)
        imageView.setTag(R.id.code_tag_name, code)
        imageView.setTag(R.id.resid_tag_name, resourceId)
        val type = when (imageView.id) {
            R.id.btn_main -> viewModel.MAIN
            R.id.btn_secondary1 -> viewModel.SEC1
            R.id.btn_secondary2 -> viewModel.SEC2
            else -> viewModel.MAIN
        }
        fetchCurrencyRates(type)
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
        play()
    }

    private val onActionClickListener = View.OnClickListener {
        if (txvResult.text.isNotEmpty()) {
            operation(getAction(it.id))
            isActionSelected = true
        }
        play()
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
        play()
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
        play()
    }
    private val onClearClickListener = View.OnClickListener {
        val1 = Double.NaN
        val2 = Double.NaN
        SELECTED_ACTION = ' '
        txvResult.text = "0"
        savedMainVal = .0
        unselectSecondary()
        secondarySelectedId = 0
        play()
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
                convertToSec(it.getTag(R.id.code_tag_name).toString())

            } else if (secondarySelectedId == it.id) {
                secondarySelectedId = 0
                convertBack()
            }
        }
    }

    private fun convertToSec(secCode: String) {
//        Log.e(TAG, "convertToSec: $secCode")
        if (viewModel.currencyMainList.value != null) {
            val rate = viewModel.currencyMainList.value!!.filter { it.code == secCode }[0].rate
            val converted = savedMainVal * rate
//        Log.e(TAG, "savedMainVal: $savedMainVal, rate: $rate, converted: $converted")
            txvResult.setResult(converted)
            val1 = txvResult.text.toString().toDouble()
        }
    }

    private fun convertBack() {
        Log.e(TAG, "convertBack savedMainVal: $savedMainVal")
        txvResult.setResult(savedMainVal)
        val1 = txvResult.text.toString().toDouble()
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
        txvResult = findViewById(R.id.txtResult)

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
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        World.init(applicationContext)

        viewSetup()

        setCurrencyBtn()

        viewModel = CurrencyListViewModel(application, DataRepository())

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

        setCur(btn_main, "USD")
        setCur(btn_secondary1, "AED")
        setCur(btn_secondary2, "KZT")

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


        var sharedPref =
            getSharedPreferences(getString(R.string.preference_file), Context.MODE_PRIVATE)
        val favourites = sharedPref?.getString(getString(R.string.saved_favourites_key), "") ?: ""
        val isTutorialViewed =
            sharedPref?.getBoolean(getString(R.string.saved_is_totorial_key), false) ?: false

        favList = favourites.split(",").filter { it != "" }.toMutableList()

        if (!isTutorialViewed) {
            with(sharedPref!!.edit()) {
                putBoolean(getString(R.string.saved_is_totorial_key), true)
                commit()
            }
            val intent = Intent(this, TutorialActivity::class.java)
            startActivity(intent)
        }
        mp = MediaPlayer.create(this, R.raw.sound)

    }

    fun play() {
        try {
            if (mp!!.isPlaying) {
                mp!!.stop()
                mp!!.release()
                mp = MediaPlayer.create(this, R.raw.sound)
            }
            mp!!.start()
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }
    }

    private fun setCurrencyBtn() {
        txvResult.setOnTouchListener(object : OnSwipeListener(this) {
            override fun onSwipeRight() {
                super.onSwipeRight()
                Log.e("MainActivity", "txvResult onSwipeRight")
                txvResult.swipe()
            }

            override fun onSwipeLeft() {
                super.onSwipeLeft()
                Log.e("MainActivity", "txvResult onSwipeLeft")
                txvResult.swipe()
            }
        })

        btn_main.setOnTouchListener(object : OnSwipeListener(this) {
            override fun onSwipeRight() {
                super.onSwipeRight()
                switchMain(secondarySelectedId)
            }

            override fun onSwipeTop() {
                super.onSwipeTop()
                scrollCurrency(btn_main, UP)
            }

            override fun onSwipeBottom() {
                super.onSwipeBottom()
                Log.e("MainActivity", "onSwipeBottom")
                scrollCurrency(btn_main, DOWN)
            }

            override fun onLongPress() {
                super.onLongPress()
                Log.e("MainActivity", "View On Long Click!!!!")
                longClickedId = R.id.btn_main
                showDialog()
            }
        })
        btn_secondary1.setOnTouchListener(object : OnSwipeListener(this) {

            override fun onSwipeLeft() {
                super.onSwipeLeft()
                switchMain(R.id.btn_secondary1)
            }

            override fun onSwipeTop() {
                super.onSwipeTop()
                Log.e("MainActivity", "onSwipeTop")
                scrollCurrency(btn_secondary1, UP)
            }

            override fun onSwipeBottom() {
                super.onSwipeBottom()
                Log.e("MainActivity", "onSwipeBottom")
                scrollCurrency(btn_secondary1, DOWN)
            }

            override fun onLongPress() {
                super.onLongPress()
                Log.e("MainActivity", "View On Long Click!!!!")
                longClickedId = R.id.btn_secondary1
                showDialog()
            }
        })
        btn_secondary2.setOnTouchListener(object : OnSwipeListener(this) {

            override fun onSwipeLeft() {
                super.onSwipeLeft()
                switchMain(R.id.btn_secondary2)
            }

            override fun onSwipeTop() {
                super.onSwipeTop()
                Log.e("MainActivity", "onSwipeTop")
                scrollCurrency(btn_secondary2, UP)
            }

            override fun onSwipeBottom() {
                super.onSwipeBottom()
                Log.e("MainActivity", "onSwipeBottom")
                scrollCurrency(btn_secondary2, DOWN)
            }

            override fun onLongPress() {
                super.onLongPress()
                Log.e("MainActivity", "View On Long Click!!!!")
                longClickedId = R.id.btn_secondary2
                showDialog()
            }
        })
    }

    private fun scrollCurrency(btn: ImageView, scrollType: Int) {
        var sharedPref =
            getSharedPreferences(getString(R.string.preference_file), Context.MODE_PRIVATE)
        val favourites = sharedPref?.getString(getString(R.string.saved_favourites_key), "") ?: ""

        favList = favourites.split(",").filter { it != "" }.toMutableList()

        if (favList.isNotEmpty()) {
            val code = btn.getTag(R.id.code_tag_name)
            var index = favList.indexOf(code)
            val toCode =
                if (scrollType == DOWN) {
                    favList[if (index == favList.size - 1) 0 else index + 1]
                } else {
                    favList[if (index == 0) favList.size - 1 else index - 1]
                }
            setCur(btn, toCode)
            if (btn.id != R.id.btn_main && secondarySelectedId != 0) {
                convertToSec(toCode)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Hide the status bar.
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN
        // Remember that you should never show the action bar if the
        // status bar is hidden, so hide that too if necessary.
        actionBar?.hide()
        hideSystemUI()
    }

    private fun hideSystemUI() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).let { controller ->
            controller.hide(WindowInsetsCompat.Type.systemBars())
            controller.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }

    private fun switchMain(secId: Int = 0) {
        if (secId != 0 && isMainSelected) {
            val mainCurrencyCode = btn_main.getTag(R.id.code_tag_name) as String
            if (secId == R.id.btn_secondary1) {
                val sCurrencyCode = btn_secondary1.getTag(R.id.code_tag_name) as String
                setCur(btn_main, sCurrencyCode)
                setCur(btn_secondary1, mainCurrencyCode)
            } else {
                val sCurrencyCode = btn_secondary2.getTag(R.id.code_tag_name) as String
                setCur(btn_main, sCurrencyCode)
                setCur(btn_secondary2, mainCurrencyCode)
            }
            if (secondarySelectedId != 0) {
                unselectSecondary()
                secondarySelectedId = 0
            }
        }
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
        val sharedPref = getSharedPreferences(
            getString(R.string.preference_file),
            Context.MODE_PRIVATE
        )
        var favourites = sharedPref?.getString(getString(R.string.saved_favourites_key), "") ?: ""
        favList = favourites.split(",").filter { it != "" }.toMutableList()
        var list = when (longClickedId) {
            R.id.btn_main -> viewModel.currencyMainList.value?.toMutableList()
            R.id.btn_secondary1 -> viewModel.currencySec1List.value?.toMutableList()
            R.id.btn_secondary2 -> viewModel.currencySec2List.value?.toMutableList()
            else -> viewModel.currencyMainList.value?.toMutableList()
        }

        list!!.find { favList.contains(it.code) }?.isFavorite2 = true
        list!!.sortBy { !it.isFavorite2 }

        newFragment.list = list
        newFragment.show(supportFragmentManager, "dialog")
    }
}