package com.thecalcurate.android

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.content.Context
import android.media.MediaPlayer
import android.os.*
import android.util.Log
import android.view.View
import android.view.ViewGroup
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
import com.thecalcurate.android.ui.*
import com.thecalcurate.android.viewmodel.CurrencyListViewModel


class MainActivity : AppCompatActivity(), CurrencyDialog.NoticeDialogListener {
    val TAG = "MainActivity"
    lateinit var b0: View
    lateinit var b1: View
    lateinit var b2: View
    lateinit var b3: View
    lateinit var b4: View
    lateinit var b5: View
    lateinit var b6: View
    lateinit var b7: View
    lateinit var b8: View
    lateinit var b9: View

    lateinit var b_equal: View
    lateinit var b_multi: View
    lateinit var b_divide: View
    lateinit var b_add: View
    lateinit var b_sub: View
    lateinit var b_percent: View
    lateinit var b_dot: View
    lateinit var b_clear: View

    lateinit var blurView: View
    lateinit var btn_main: ImageView
    lateinit var btn_secondary1: ImageView
    lateinit var btn_secondary2: ImageView

    //Tutorial Views
    lateinit var btn_secondary1_tut: ImageView
    lateinit var btn_secondary2_tut: ImageView
    lateinit var txvHold: View
    lateinit var imgHold: View
    lateinit var txvSwipe: View
    lateinit var txvSwipeRight: View
    lateinit var txvSwipeLeft: View
    lateinit var imvCloseTutorial: View


    lateinit var txvResult: MainTextView
    lateinit var imvResult: ImageView

    private val ADDITION = '+'
    private val SUBTRACTION = '-'
    private val MULTIPLICATION = '*'
    private val DIVISION = '/'
    private val EQU = '='

    private val PERCENT = '%'
    private var SELECTED_ACTION = ' '
    private var isActionSelected = false
    private var selectedActionView: View? = null
    private var val1 = Double.NaN
    private var val2 = Double.NaN

    private var longClickedId = 0

    var isMainSelected = false
    var savedMainVal = .0
    var secondarySelectedId = 0

    var tutorialStep = 1
    var isTutorialViewed = false

    lateinit var viewModel: CurrencyListViewModel
    private lateinit var networkMonitor: com.thecalcurate.android.data.NetworkMonitor
    private val bannerCollapseHandler = android.os.Handler(android.os.Looper.getMainLooper())
    private var bannerCollapseRunnable: Runnable? = null
    lateinit var favList: MutableList<String>
    lateinit var selectedCurList: MutableList<String>
    var mp: MediaPlayer? = null
    val VIBRATION_MILIS = 100L
    // Matches iOS UIImpactFeedbackGenerator(style: .heavy) — short, sharp pulse.
    val HAPTIC_HEAVY_MILIS = 35L

    var itemClickListener = object : CurrencyRecyclerViewAdapter.ItemClickListener {
        override fun onItemClick(view: View?, position: Int, isCrypto: Boolean) {
            var curItem = view?.tag as CurrencyItem
            when (longClickedId) {
                R.id.btn_main -> setCur(btn_main, curItem.code)
                R.id.btn_secondary1 -> setCur(btn_secondary1, curItem.code)
                R.id.btn_secondary2 -> setCur(btn_secondary2, curItem.code)
            }
            if (secondarySelectedId == longClickedId) {
                isActionSelected = false
                val1 = Double.NaN
                txvResult.setResult(convertToSec(savedMainVal, curItem.code))
            }
            if (!isTutorialViewed && tutorialStep == 1) {
                tutorialStep++
                nextTutorialStep()
            }
//            hideKeyboard()
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
        val resourceId = if (com.thecalcurate.android.model.CryptoItem.isCryptoCode(code)) {
            com.thecalcurate.android.model.CryptoItem.getList().first { it.code == code }.iconResId
        } else {
            CurrencyButton.getResource(code)
        }
        imageView.setImageResource(resourceId)
        imageView.setTag(R.id.code_tag_name, code)
        imageView.setTag(R.id.resid_tag_name, resourceId)
        val type = when (imageView.id) {
            R.id.btn_main -> viewModel.MAIN
            R.id.btn_secondary1 -> viewModel.SEC1
            R.id.btn_secondary2 -> viewModel.SEC2
            else -> viewModel.MAIN
        }
        selectedCurList[type - 1] = code
        fetchCurrencyRates(type)
    }

    var newFragment = CurrencyDialog(itemClickListener)

    private val onNumberClickListener = View.OnClickListener {
        try {
            play()
            pressFlash(it, 100L)
            val number = getNumberClicked(it.id)
            ifErrorOnOutput()

            if (isActionSelected || txvResult.text.toString() == "0") {
                txvResult.text = number.toString()
                selectedActionView?.isSelected = false
                isActionSelected = false
            } else {
                txvResult.text = txvResult.text.toString() + number
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private val onDotClickListener = View.OnClickListener {
        try {
            play()
            pressFlash(it, 100L)
            ifErrorOnOutput()

            if (isActionSelected || txvResult.text.toString() == "0") {
                txvResult.text = "0."
                isActionSelected = false
                selectedActionView?.isSelected = false
            } else if (!txvResult.text.toString().contains(".")) {
                txvResult.text = txvResult.text.toString() + "."
            }

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    private val onActionClickListener = View.OnClickListener {
        try {
            play()
            pressFlash(it, 200L)
            if (txvResult.text.isNotEmpty()) {
                val action = getAction(it.id)
                operation(getAction(it.id))
                selectedActionView?.isSelected = false
                if (action != EQU) {
                    it.isSelected = true
                    selectedActionView = it
                }
                isActionSelected = true
                isEqualPressed = false
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private val onPercentageClickListener = View.OnClickListener {
        play()
        pressFlash(it, 200L)
        if (txvResult.text.isNotEmpty()) {
            val result = if (!val1.isNaN()) {
                val2 = txvResult.text.toString().filter { it.isDigit() || it=='.' }.toDouble()
                if (SELECTED_ACTION == MULTIPLICATION || SELECTED_ACTION == DIVISION) {
                    val2 *= 0.01
                    val2
                } else {
                    val1 * (0.01 * val2)
                }
            } else {
                val1 = .0
                val1 * (0.01 * val2)
            }
            showResult(result)
        }
    }

    private val onEqualClickListener = View.OnClickListener {
        try {
            play()
            pressFlash(it, 200L)
            if (txvResult.text.isNotEmpty()) {
                if (!val1.isNaN() && SELECTED_ACTION != ' ') {
                    calculateResult()
                    selectedActionView?.isSelected = false
                    isActionSelected = true
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    var isEqualPressed = false
    private fun calculateResult() {
        if (!isEqualPressed)
            val2 = txvResult.text.toString().filter { it.isDigit() || it=='.' }.toDouble()
        Log.i(TAG, "onEqualClickListener val1: $val1, val2: $val2")
        val result = calculate(val1, val2, SELECTED_ACTION)
        showResult(result)
        val1 = result
        isEqualPressed = true
        calculateMainVal()
    }

    private val onClearClickListener = View.OnClickListener {
        try {
            play()
            pressFlash(it, 200L)
            val1 = Double.NaN
            val2 = Double.NaN
            isEqualPressed = false
            SELECTED_ACTION = ' '
            selectedActionView?.isSelected = false
            txvResult.text = "0"
            savedMainVal = .0
            unselectSecondary()
            secondarySelectedId = 0
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }


    private val onMainClickListener = View.OnClickListener {
        try {
            Log.e("MainActivity", "main on click, it.isSelected: " + it.isSelected)
            it.isSelected = !it.isSelected
            isMainSelected = it.isSelected
            if (!isMainSelected) {
                unselectSecondary()
                secondarySelectedId = 0
            }
        } catch (e: Exception) {
            e.printStackTrace()
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
        try {
            isActionSelected = false
            Log.e(TAG, "onSecondaryClickListener isMainSelected: $isMainSelected")
            if (isMainSelected) {
                if (secondarySelectedId == 0) {
                    savedMainVal = txvResult.getResult()
                }
                unselectSecondary()
                if (secondarySelectedId != it.id) {
                    secondarySelectedId = it.id
                    it.isSelected = true
                    txvResult.setResult(
                        convertToSec(
                            savedMainVal,
                            it.getTag(R.id.code_tag_name).toString()
                        )
                    )
                    val1 = Double.NaN
                } else if (secondarySelectedId == it.id) {
                    secondarySelectedId = 0
                    convertBack()
                    val1 = Double.NaN
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    private val onCloseTutorialClickListener = View.OnClickListener {
        completeTutorial()
    }

    private fun convertToSec(value: Double, secCode: String): Double {
        Log.e(TAG, "value: $value ,convertToSec: $secCode")
        // If the secondary slot is a crypto, look up rate from cryptoRates map.
        // Mirrors iOS CryptoChoice.convertFromBase (CurrencyEnums.swift:72).
        if (com.thecalcurate.android.model.CryptoItem.isCryptoCode(secCode)) {
            var rate = viewModel.cryptoRates.value?.get(secCode) ?: .0
            if (rate == .0) rate = 1.0
            return value * rate
        }
        if (viewModel.currencyMainList.value != null) {
            var rate = viewModel.currencyMainList.value!!.filter { it.code == secCode }[0].rate
            if (rate == .0)
                rate = 1.0
            return value * rate
//        Log.e(TAG, "savedMainVal: $savedMainVal, rate: $rate, converted: $converted")
//            if (txvResult.text.toString() != "0")
//                val1 = txvResult.text.toString().toDouble()
        }
        return value
    }

    private fun getSecView(secId: Int): View {
        return if (secId == R.id.btn_secondary1) btn_secondary1 else btn_secondary2
    }

    private fun calculateMainVal() {
        if (secondarySelectedId != 0 && viewModel.currencyMainList.value != null) {
            val secCode = getSecView(secondarySelectedId).getTag(R.id.code_tag_name).toString()
            var rate: Double = if (com.thecalcurate.android.model.CryptoItem.isCryptoCode(secCode)) {
                viewModel.cryptoRates.value?.get(secCode) ?: .0
            } else {
                viewModel.currencyMainList.value!!.filter { it.code == secCode }[0].rate
            }
            if (rate == .0)
                rate = 1.0
            savedMainVal = txvResult.getResult() / rate
            Log.e(TAG, "calculateMainVal savedMainVal: $savedMainVal")
//        Log.e(TAG, "savedMainVal: $savedMainVal, rate: $rate, converted: $converted")
//            if (txvResult.text.toString() != "0")
//                val1 = txvResult.text.toString().toDouble()
        }
    }

    private fun convertBack() {
        Log.e(TAG, "convertBack savedMainVal: $savedMainVal")
        txvResult.setResult(savedMainVal)
//        if (txvResult.text.toString() != "0")
//            val1 = txvResult.text.toString().toDouble()
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

    lateinit var vibe: Vibrator

    private fun viewSetup() {
        try {
            txvResult = findViewById(R.id.txtResult)
            imvResult = findViewById(R.id.imvResult)

            btn_main = findViewById(R.id.btn_main)
            btn_secondary1 = findViewById(R.id.btn_secondary1)
            btn_secondary2 = findViewById(R.id.btn_secondary2)

            //Tutorial Views
            txvHold = findViewById(R.id.txvHold)
            imgHold = findViewById(R.id.imgHold)
            btn_secondary1_tut = findViewById(R.id.btn_secondary1_tut)
            btn_secondary2_tut = findViewById(R.id.btn_secondary2_tut)
            txvSwipe = findViewById(R.id.txvSwipe)
            txvSwipeRight = findViewById(R.id.txvSwipeRight)
            txvSwipeLeft = findViewById(R.id.txvSwipeLeft)
            imvCloseTutorial = findViewById(R.id.imvCloseTutorial)

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

            blurView = findViewById(R.id.blurView)
            b_dot = findViewById(R.id.btn_dot)
            b_equal = findViewById(R.id.btn_equal)
            b_multi = findViewById(R.id.btn_multi)
            b_divide = findViewById(R.id.btn_divide)
            b_add = findViewById(R.id.btn_add)
            b_sub = findViewById(R.id.btn_sub)
            b_clear = findViewById(R.id.btn_clear)
            b_percent = findViewById(R.id.btn_percent)

            vibe = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager =
                    getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                vibratorManager.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                getSystemService(VIBRATOR_SERVICE) as Vibrator
            }

            var radius = 1f

            var decorView = window.decorView
            // ViewGroup you want to start blur from. Choose root as close to BlurView in hierarchy as possible.
            var rootView: ViewGroup = decorView.findViewById(android.R.id.content)

            // Optional:
            // Set drawable to draw in the beginning of each blurred frame.
            // Can be used in case your layout has a lot of transparent space and your content
            // gets a too low alpha value after blur is applied.
            var windowBackground = decorView.background


            if (!isTutorialViewed) {
//            blurView.setupWith(rootView, RenderScriptBlur(this)) // or RenderEffectBlur
//                .setFrameClearDrawable(windowBackground) // Optional
//                .setBlurRadius(radius)
                blurView.setOnClickListener(null)
                blurView.visibility = View.GONE

//            btn_secondary1.visibility = View.GONE
//            btn_secondary2.visibility = View.GONE
//            btn_secondary1_tut.visibility = View.VISIBLE
//            btn_secondary2_tut.visibility = View.VISIBLE

//            fadeIn(blurView, 3000L)
                fadeIn(txvHold, 3000L)
                fadeIn(imgHold, 3000L)
            } else {
                blurView.visibility = View.GONE
//            btn_secondary1.visibility = View.VISIBLE
//            btn_secondary2.visibility = View.VISIBLE

                txvHold.visibility = View.GONE
                imgHold.visibility = View.GONE
//            btn_secondary1_tut.visibility = View.GONE
//            btn_secondary2_tut.visibility = View.GONE

            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        World.init(applicationContext)

        var sharedPref =
            getSharedPreferences(getString(R.string.preference_file), Context.MODE_PRIVATE)
        val favourites = sharedPref?.getString(getString(R.string.saved_favourites_key), "") ?: ""
        val selectedCur =
            sharedPref?.getString("selected_currencies_key", "USD,EUR,GBP")
                ?: "USD,EUR,GBP"
        isTutorialViewed =
            sharedPref?.getBoolean(getString(R.string.saved_is_totorial_key), false) ?: false

        viewSetup()

        setCurrencyBtn()

        viewModel = CurrencyListViewModel(application, DataRepository())

        viewModel.errorMessage.observe(this) {
            Toast.makeText(this, it, Toast.LENGTH_SHORT).show()
        }

        networkMonitor = com.thecalcurate.android.data.NetworkMonitor(this)
        networkMonitor.start()
        networkMonitor.quality.observe(this) { quality ->
            applyNetworkBanner(quality)
        }
        // Tap collapsed banner → re-expand description for another 15s.
        findViewById<View>(R.id.networkBanner).setOnClickListener {
            findViewById<View>(R.id.bannerDesc).visibility = View.VISIBLE
            scheduleBannerCollapse()
        }

        // When fresh crypto rates arrive, recompute the selected secondary if it's a crypto slot.
        viewModel.cryptoRates.observe(this) {
            if (secondarySelectedId != 0) {
                val secCode = getSecView(secondarySelectedId).getTag(R.id.code_tag_name)?.toString()
                if (secCode != null && com.thecalcurate.android.model.CryptoItem.isCryptoCode(secCode)) {
                    txvResult.setResult(convertToSec(savedMainVal, secCode))
                }
            }
        }

        viewModel.loading.observe(this, Observer {
//            if (it) {
//                binding.progressDialog.visibility = View.VISIBLE
//            } else {
//                binding.progressDialog.visibility = View.GONE
//            }
        })


        selectedCurList = selectedCur.split(",").filter { it != "" }.toMutableList()

        setCur(btn_main, selectedCurList[0])
        setCur(btn_secondary1, selectedCurList[1])
        setCur(btn_secondary2, selectedCurList[2])
        setCur(btn_secondary1_tut, selectedCurList[1])
        setCur(btn_secondary2_tut, selectedCurList[2])

        btn_main.setOnClickListener(onMainClickListener)
        btn_secondary1.setOnClickListener(onSecondaryClickListener)
        btn_secondary2.setOnClickListener(onSecondaryClickListener)
        imvCloseTutorial.setOnClickListener(onCloseTutorialClickListener)

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
        b_dot.setOnClickListener(onDotClickListener)

        b_percent.setOnClickListener(onPercentageClickListener)
        b_add.setOnClickListener(onActionClickListener)
        b_sub.setOnClickListener(onActionClickListener)
        b_multi.setOnClickListener(onActionClickListener)
        b_divide.setOnClickListener(onActionClickListener)

        b_equal.setOnClickListener(onEqualClickListener)
        b_clear.setOnClickListener(onClearClickListener)

        favList = favourites.split(",").filter { it != "" }.toMutableList()

//        if (!isTutorialViewed) {
//            with(sharedPref!!.edit()) {
//                putBoolean(getString(R.string.saved_is_totorial_key), true)
//                commit()
//            }
//            val intent = Intent(this, TutorialActivity::class.java)
//            startActivity(intent)
//        }

        if (!isTutorialViewed) {
            btn_main.callOnClick()
        }

        mp = MediaPlayer.create(this, R.raw.sound)
    }

    override fun onPause() {
        var sharedPref =
            getSharedPreferences(getString(R.string.preference_file), Context.MODE_PRIVATE)

        var selected = selectedCurList.joinToString(",") { it }

//        Log.e(TAG, "onPause selected: $selected")
        with(sharedPref!!.edit()) {
            putString("selected_currencies_key", selected)
            commit()
        }
        super.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (this::networkMonitor.isInitialized) networkMonitor.stop()
        bannerCollapseRunnable?.let { bannerCollapseHandler.removeCallbacks(it) }
    }

    private fun applyNetworkBanner(quality: com.thecalcurate.android.data.NetworkQuality) {
        val banner = findViewById<View>(R.id.networkBanner)
        val icon = findViewById<androidx.appcompat.widget.AppCompatImageView>(R.id.bannerIcon)
        val title = findViewById<android.widget.TextView>(R.id.bannerTitle)
        val desc = findViewById<android.widget.TextView>(R.id.bannerDesc)
        when (quality) {
            com.thecalcurate.android.data.NetworkQuality.NONE -> {
                banner.setBackgroundResource(R.drawable.banner_bg_red)
                title.text = getString(R.string.banner_no_connection_title)
                desc.text = getString(R.string.banner_no_connection_desc)
                title.setTextColor(android.graphics.Color.WHITE)
                desc.setTextColor(android.graphics.Color.WHITE)
                icon.setColorFilter(android.graphics.Color.WHITE)
                desc.visibility = View.VISIBLE
                banner.visibility = View.VISIBLE
                scheduleBannerCollapse()
            }
            com.thecalcurate.android.data.NetworkQuality.METERED -> {
                banner.setBackgroundResource(R.drawable.banner_bg_yellow)
                title.text = getString(R.string.banner_bad_title)
                desc.text = getString(R.string.banner_bad_desc)
                // iOS yellow banner uses black foreground (icon + text) for contrast.
                title.setTextColor(android.graphics.Color.BLACK)
                desc.setTextColor(android.graphics.Color.BLACK)
                icon.setColorFilter(android.graphics.Color.BLACK)
                desc.visibility = View.VISIBLE
                banner.visibility = View.VISIBLE
                scheduleBannerCollapse()
            }
            com.thecalcurate.android.data.NetworkQuality.HEALTHY -> {
                banner.visibility = View.GONE
                bannerCollapseRunnable?.let { bannerCollapseHandler.removeCallbacks(it) }
            }
        }
    }

    private fun scheduleBannerCollapse() {
        bannerCollapseRunnable?.let { bannerCollapseHandler.removeCallbacks(it) }
        val r = Runnable {
            findViewById<View>(R.id.bannerDesc).visibility = View.GONE
        }
        bannerCollapseRunnable = r
        bannerCollapseHandler.postDelayed(r, 15_000)
    }

    fun play() {
        try {
            if (mp!!.isPlaying) {
                mp!!.stop()
                mp!!.prepare()
//                mp!!.release()
//                mp = MediaPlayer.create(this, R.raw.sound)
            }
            mp!!.start()
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }
    }

    private fun setCurrencyBtn() {
        try {
            imvResult.setOnTouchListener(object : OnSwipeListener(this) {
                override fun onSwipeRight() {
                    super.onSwipeRight()
                    txvResult.swipe()
//                if (!val1.isNaN()) {
//                    val1 = txvResult.text.toString().toDouble()
//                }
                }

                override fun onSwipeLeft() {
                    super.onSwipeLeft()
                    txvResult.swipe()
//                if (!val1.isNaN()) {
//                    val1 = txvResult.text.toString().toDouble()
//                }
                }
            })

            btn_main.setOnTouchListener(object : OnSwipeListener(this) {
                override fun onSwipeRight() {
                    super.onSwipeRight()
                    switchMain(secondarySelectedId)
                    if (!isTutorialViewed && tutorialStep == 2) {
                        tutorialStep++
                        nextTutorialStep()
                    }
                }

                override fun onLongPress() {
                    super.onLongPress()
//                Log.e("MainActivity", "View On Long Click!!!!")
                    longClickedId = R.id.btn_main
                    showDialog()
                }
            })
            btn_secondary1.setOnTouchListener(object : OnSwipeListener(this) {

                override fun onSwipeLeft() {
                    super.onSwipeLeft()
//                if (isTutorialViewed)
                    switchMain(R.id.btn_secondary1)
                }

                override fun onLongPress() {
                    super.onLongPress()
//                if (isTutorialViewed) {
                    longClickedId = R.id.btn_secondary1
                    showDialog()
//                }
                }
            })
            btn_secondary2.setOnTouchListener(object : OnSwipeListener(this) {

                override fun onSwipeLeft() {
                    super.onSwipeLeft()
                    switchMain(R.id.btn_secondary2)
                    if (!isTutorialViewed && tutorialStep == 2) {
                        tutorialStep++
                        nextTutorialStep()
                    }
                }

                override fun onLongPress() {
                    super.onLongPress()
//                if (isTutorialViewed) {
                    longClickedId = R.id.btn_secondary2
                    showDialog()
//                }
                }
            })
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun nextTutorialStep() {
        if (tutorialStep == 2) {
            txvHold.visibility = View.GONE
            imgHold.visibility = View.GONE
//            btn_secondary1.visibility = View.VISIBLE
//            btn_secondary2.visibility = View.VISIBLE
//            txvSwipe.visibility = View.VISIBLE
////            txvSwipeRight.visibility = View.VISIBLE
//            txvSwipeLeft.visibility = View.VISIBLE

            fadeOut(txvHold)
            fadeOut(imgHold)

            btn_secondary2.callOnClick()
            btn_secondary2_tut.callOnClick()

            fadeIn(btn_secondary2)
            fadeIn(txvSwipe)
            fadeIn(txvSwipeLeft)
            fadeIn(txvSwipeRight)

        } else if (tutorialStep == 3) {
            // Scroll-step was removed (iOS doesn't have it); step 3 now finishes the tutorial.
            fadeOut(txvSwipe)
            fadeOut(txvSwipeLeft)
            fadeOut(txvSwipeRight)
            fadeIn(imvCloseTutorial)
        }
    }

    private fun completeTutorial() {
        var sharedPref =
            getSharedPreferences(getString(R.string.preference_file), Context.MODE_PRIVATE)
        with(sharedPref!!.edit()) {
            putBoolean(getString(R.string.saved_is_totorial_key), true)
            commit()
        }
        isTutorialViewed = true

        fadeOut(imvCloseTutorial)
        fadeOut(blurView, 1000L)

        btn_secondary1_tut.visibility = View.GONE
        btn_secondary2_tut.visibility = View.GONE
        b_clear.callOnClick()
    }

    private fun fadeOut(view: View, animationDuration: Long = 600L) {
        view.visibility = View.VISIBLE
        view.animate()
            .alpha(0f)
            .setDuration(animationDuration)
            .setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    view.visibility = View.GONE
                }
            })
    }

    private fun fadeIn(view: View, animationDuration: Long = 600L) {
        view.apply {
            // Set the content view to 0% opacity but visible, so that it is visible
            // (but fully transparent) during the animation.
            alpha = 0f
            visibility = View.VISIBLE

            // Animate the content view to 100% opacity, and clear any animation
            // listener set on the view.
            animate()
                .alpha(1f)
                .setDuration(animationDuration)
                .setListener(null)
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
            vibrate()
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
                    calculateResult()
                } else {
                    val1 = txvResult.text.toString().filter { it.isDigit() || it == '.' }.toDouble()
                }
            }
            SELECTED_ACTION = ACTION
        } catch (e: Exception) {
            e.printStackTrace()
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

    private fun vibrate() {
        try {
            if (Build.VERSION.SDK_INT >= 26) {
                vibe.vibrate(
                    VibrationEffect.createOneShot(
                        VIBRATION_MILIS,
                        VibrationEffect.DEFAULT_AMPLITUDE
                    )
                )
            } else {
                vibe.vibrate(VIBRATION_MILIS)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Mirrors iOS press animation (NumberView.swift:51 / ActionView.swift:91).
     * Quick alpha dip to ~0.55 and back to 1.0 over [totalMs] total.
     */
    private fun pressFlash(view: View, totalMs: Long) {
        val half = totalMs / 2
        view.animate().cancel()
        view.animate()
            .alpha(0.55f)
            .setDuration(half)
            .withEndAction {
                view.animate()
                    .alpha(1.0f)
                    .setDuration(half)
                    .start()
            }
            .start()
    }

    /** Short sharp pulse to mimic iOS UIImpactFeedbackGenerator(style: .heavy). */
    private fun hapticHeavy() {
        try {
            if (Build.VERSION.SDK_INT >= 26) {
                vibe.vibrate(
                    VibrationEffect.createOneShot(
                        HAPTIC_HEAVY_MILIS,
                        VibrationEffect.DEFAULT_AMPLITUDE
                    )
                )
            } else {
                vibe.vibrate(HAPTIC_HEAVY_MILIS)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun showDialog() {
        try {
            val type = when (longClickedId) {
                R.id.btn_main -> viewModel.MAIN
                R.id.btn_secondary1 -> viewModel.SEC1
                R.id.btn_secondary2 -> viewModel.SEC2
                else -> viewModel.MAIN
            }
            fetchCurrencyRates(type)

            hapticHeavy()

            val sharedPref = getSharedPreferences(
                getString(R.string.preference_file),
                Context.MODE_PRIVATE
            )
            var favourites =
                sharedPref?.getString(getString(R.string.saved_favourites_key), "") ?: ""
            favList = favourites.split(",").filter { it != "" }.toMutableList()

            var list = when (longClickedId) {
                R.id.btn_main -> viewModel.currencyMainList.value?.toMutableList()
                R.id.btn_secondary1 -> viewModel.currencySec1List.value?.toMutableList()
                R.id.btn_secondary2 -> viewModel.currencySec2List.value?.toMutableList()
                else -> viewModel.currencyMainList.value?.toMutableList()
            }

            var allFavSorted = favList.map { favItem ->
                var d = list!!.find { it.code == favItem }
                d!!.isFavorite2 = true
                d
            }

            var listToShow = list!!.filter { !favList.contains(it.code) }.toMutableList()
            listToShow.addAll(0, allFavSorted)

            newFragment.listToShow = listToShow
            newFragment.list = list
            newFragment.cryptoRates = viewModel.cryptoRates.value ?: emptyMap()
            // If the slot we're editing currently holds a crypto code, open the picker on the Crypto tab.
            val activeCode = when (longClickedId) {
                R.id.btn_main -> selectedCurList.getOrNull(0)
                R.id.btn_secondary1 -> selectedCurList.getOrNull(1)
                R.id.btn_secondary2 -> selectedCurList.getOrNull(2)
                else -> null
            }
            newFragment.openOnCryptoTab =
                activeCode != null && com.thecalcurate.android.model.CryptoItem.isCryptoCode(activeCode)
            newFragment.show(supportFragmentManager, "dialog")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}