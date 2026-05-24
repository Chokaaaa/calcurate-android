package com.thecalcurate.android.ui

import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.CheckBox
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.AppCompatEditText
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.tabs.TabLayout
import com.thecalcurate.android.R
import com.thecalcurate.android.model.CryptoItem
import com.thecalcurate.android.model.CurrencyItem

class CurrencyDialog(
    val itemClickListener: CurrencyRecyclerViewAdapter.ItemClickListener
) :
    DialogFragment() {
    // Use this instance of the interface to deliver action events
    private lateinit var listener: NoticeDialogListener
    private var adapter: CurrencyRecyclerViewAdapter? = null

    private var sharedPref = activity?.getPreferences(Context.MODE_PRIVATE)
    private lateinit var favourites: String
    private lateinit var favList: MutableList<String>
    private val TAG = "CurrencyDialog"

    var listToShow: MutableList<CurrencyItem>? = null
    var list: MutableList<CurrencyItem>? = null
    lateinit var imvClear: View
    lateinit var edtSearch: EditText

    /** Populated by MainActivity from viewModel.cryptoRates before show(). */
    var cryptoRates: Map<String, Double> = emptyMap()

    private var isCryptoTab: Boolean = false

    /* The activity that creates an instance of this dialog fragment must
     * implement this interface in order to receive event callbacks.
     * Each method passes the DialogFragment in case the host needs to query it. */
    interface NoticeDialogListener {
        fun onDialogNegativeClick(dialog: DialogFragment)
    }

    val textChangeListener = object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
        }

        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
        }

        override fun afterTextChanged(editable: Editable?) {
            // Search filters the currently-active tab's source list (fiat OR crypto).
            val source: MutableList<CurrencyItem> =
                if (isCryptoTab) buildCryptoRows() else listToShow!!
            if (editable.toString().isEmpty()) {
                adapter?.setList(source)
                imvClear.visibility = View.GONE
            } else {
                adapter?.setList(source.filter { curItem ->
                    curItem.name.contains(editable.toString(), true) ||
                        curItem.code.contains(editable.toString(), true)
                }.toMutableList())
            }
            adapter?.notifyDataSetChanged()

            if (editable?.isNotEmpty() == true && imvClear.visibility == View.GONE) {
                imvClear.visibility = View.VISIBLE
            }
        }
    }

    val itemClickListener2 = object : CurrencyRecyclerViewAdapter.ItemClickListener {
        override fun onItemClick(view: View?, position: Int, isCrypto: Boolean) {
            // Substitute the dialog's own tab state — the adapter doesn't know it.
            itemClickListener.onItemClick(view, position, isCryptoTab)
            dismiss()
        }
    }
    val clearClickListener = View.OnClickListener {
        edtSearch.setText("")
        imvClear.visibility = View.GONE
    }

    val checkClickListener = View.OnClickListener {
        val code = it.tag as String
        val isFavorite = (it as CheckBox).isChecked
        Log.e(TAG, "OnCheckedChangeListener code: $code, isFavourite: $isFavorite")

        var adapterList = adapter?.getList()?.toMutableList()
        if (isFavorite) {
            favList.add(0, code)
        } else {
            favList.remove(code)
        }
        adapterList = generateListToShow(adapterList!!)
        adapter?.setList(adapterList)
        adapter?.notifyDataSetChanged()
    }

    private fun generateListToShow(adapterList: MutableList<CurrencyItem> = list!!): MutableList<CurrencyItem> {
        var allFavSorted = favList.map { favItem ->
            var d = list!!.find { it.code == favItem }
            d!!.isFavorite2 = true
            d
        }

        listToShow = list!!.filter { !favList.contains(it.code) }.toMutableList()
        listToShow!!.forEach { it.isFavorite2 = false }
        listToShow!!.addAll(0, allFavSorted)

        return listToShow!!.filter { adapterList.contains(it) }.toMutableList()
    }

    /**
     * Build a list of CurrencyItem-shaped rows representing cryptos so the existing adapter can render them.
     * iconResId is set so the adapter shows the branded coin icon and hides the favorite star + rate.
     */
    private fun buildCryptoRows(): MutableList<CurrencyItem> {
        return CryptoItem.getList().map { c ->
            CurrencyItem(c.name, c.code, cryptoRates[c.code] ?: 0.0).also {
                it.isFavorite2 = false
                it.iconResId = c.iconResId
                it.ticker = c.code
            }
        }.toMutableList()
    }

    // Override the Fragment.onAttach() method to instantiate the NoticeDialogListener
    override fun onAttach(context: Context) {
        super.onAttach(context)
        // Verify that the host activity implements the callback interface
        try {
            // Instantiate the NoticeDialogListener so we can send events to the host
            listener = context as NoticeDialogListener
        } catch (e: ClassCastException) {
            // The activity doesn't implement the interface, throw exception
            throw ClassCastException(
                ("$context must implement NoticeDialogListener")
            )
        }
        sharedPref = activity?.getSharedPreferences(
            getString(R.string.preference_file),
            Context.MODE_PRIVATE
        )
        favourites = sharedPref?.getString(getString(R.string.saved_favourites_key), "") ?: ""

        Log.e(TAG, "onAttach favourites: $favourites")
        favList = favourites.split(",").filter { it != "" }.toMutableList()

        adapter = CurrencyRecyclerViewAdapter(context)
    }

    override fun onDismiss(dialog: DialogInterface) {

        if (sharedPref != null && list != null) {
            favourites = favList.joinToString(",") { it }
            //list!!.filter { it.isFavorite2 }.joinToString(",") { it.code }

            Log.e(TAG, "onDismiss favourites: $favourites")
            with(sharedPref!!.edit()) {
                putString(getString(R.string.saved_favourites_key), favourites)
                commit()
            }
        }

//        (activity as MainActivity).hideKeyboard()

        super.onDismiss(dialog)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        Log.e(TAG, "onCreateDialog favourites: $favourites")
//        dialog?.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN)
        return activity?.let { fragment ->
            val builder = AlertDialog.Builder(fragment)
            Log.e(TAG, "onCreateDialog2 (listToShow != null): ${(listToShow != null)}")
            if (listToShow != null) {
                Log.e(TAG, "onCreateDialog2 favourites: $favourites")
                // Get the layout inflater
                val inflater = requireActivity().layoutInflater;
                // Pass null as the parent view because its going in the dialog layout
                var dialogView = inflater.inflate(R.layout.currency_search, null)
                edtSearch = dialogView.findViewById<AppCompatEditText>(R.id.edtSearch)
                imvClear = dialogView.findViewById(R.id.imvClear)
                var recyclerView = dialogView.findViewById<RecyclerView>(R.id.recyclerview)
                val tabLayout = dialogView.findViewById<TabLayout>(R.id.tabLayout)
                val txvFav = dialogView.findViewById<View>(R.id.txvFav)
                val txvCurr = dialogView.findViewById<View>(R.id.txvCurr)
                val txvRates = dialogView.findViewById<View>(R.id.txvRates)

                adapter?.setList(listToShow!!)
                adapter?.setClickListener(itemClickListener2, checkClickListener)
                edtSearch.addTextChangedListener(textChangeListener)
                imvClear.setOnClickListener(clearClickListener)

                tabLayout?.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
                    override fun onTabSelected(tab: TabLayout.Tab?) {
                        isCryptoTab = (tab?.position == 1)
                        if (isCryptoTab) {
                            // Search bar stays visible (matches iOS); crypto list is just 8 rows
                            // so search is a near no-op but the bar is part of the layout.
                            adapter?.setList(buildCryptoRows())
                        } else {
                            adapter?.setList(listToShow!!)
                        }
                        adapter?.notifyDataSetChanged()
                    }
                    override fun onTabUnselected(tab: TabLayout.Tab?) {}
                    override fun onTabReselected(tab: TabLayout.Tab?) {}
                })

                recyclerView.adapter = adapter
                builder.setView(dialogView)
            }
            val dialog = builder.create()
            dialog.window?.apply {
                // Match iOS: transparent window so the rounded shape shows through, plus 50% black scrim.
                setBackgroundDrawableResource(android.R.color.transparent)
                setDimAmount(0.5f)
                // Explicit CENTER — without panel decoration the dialog otherwise
                // floats to the bottom of the screen on some devices.
                setGravity(android.view.Gravity.CENTER)
            }
            dialog
        } ?: throw IllegalStateException("Activity cannot be null")
    }
}
