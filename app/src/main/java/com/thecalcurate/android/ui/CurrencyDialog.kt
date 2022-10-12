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
import android.widget.CompoundButton
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.AppCompatEditText
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.MutableLiveData
import androidx.recyclerview.widget.RecyclerView
import com.thecalcurate.android.R
import com.thecalcurate.android.model.CurrencyItem
import okhttp3.internal.notifyAll

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

    var list: MutableList<CurrencyItem>? = null

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
            if (editable.toString().isEmpty()) {
                adapter?.setList(list!!)
            } else {
                adapter?.setList(list!!.filter { curItem ->
                    curItem.name.contains(
                        editable.toString(),
                        true
                    )
                })
            }
            adapter?.notifyDataSetChanged()
        }
    }

    val itemClickListener2 = object : CurrencyRecyclerViewAdapter.ItemClickListener {
        override fun onItemClick(view: View?, position: Int) {
            itemClickListener.onItemClick(view, position)
            dismiss()
        }
    }

    val checkClickListener = View.OnClickListener {
        val code = it.tag as String
        val isFavorite = (it as CheckBox).isChecked
        Log.e(TAG, "OnCheckedChangeListener code: $code, isFavourite: $isFavorite")

        if (isFavorite) {
            favList.add(code)
        } else {
            favList.remove(code)
        }

        var adapterList = adapter?.getList()?.toMutableList()

        list!!.find { code == it.code }?.isFavorite2 = isFavorite
        adapterList!!.find { code == it.code }?.isFavorite2 = isFavorite

        list!!.sortBy { !it.isFavorite2 }
        adapterList!!.sortBy { !it.isFavorite2 }
        adapter?.setList(adapterList!!)
        adapter?.notifyDataSetChanged()
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
        favList = favourites.split(",").filter { it != "" }.toMutableList()

        adapter = CurrencyRecyclerViewAdapter(context)
    }

    override fun onDismiss(dialog: DialogInterface) {

        if (sharedPref != null && list != null) {
            favourites =
                list!!.filter { it.isFavorite2 }.joinToString(",") { it.code }

            Log.e(TAG, "onDismiss favourites: $favourites")
            with(sharedPref!!.edit()) {
                putString(getString(R.string.saved_favourites_key), favourites)
                commit()
            }
        }

        super.onDismiss(dialog)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let { fragment ->
            val builder = AlertDialog.Builder(fragment)
            if (list != null) {
                list!!.find { favList.contains(it.code) }?.isFavorite2 = true
                list!!.sortBy { !it.isFavorite2 }
                // Get the layout inflater
                val inflater = requireActivity().layoutInflater;
                // Pass null as the parent view because its going in the dialog layout
                var dialogView = inflater.inflate(R.layout.currency_search, null)
                var edtSearch = dialogView.findViewById<AppCompatEditText>(R.id.edtSearch)
                var recyclerView = dialogView.findViewById<RecyclerView>(R.id.recyclerview)
                adapter?.setList(list!!)
                adapter?.setClickListener(itemClickListener2, checkClickListener)
                edtSearch.addTextChangedListener(textChangeListener)

                recyclerView.adapter = adapter
                builder.setView(dialogView)
            }
            builder.create()
        } ?: throw IllegalStateException("Activity cannot be null")
    }

    fun setList(currencyList: MutableLiveData<List<CurrencyItem>>) {
        list = currencyList.value?.toMutableList()
    }
}