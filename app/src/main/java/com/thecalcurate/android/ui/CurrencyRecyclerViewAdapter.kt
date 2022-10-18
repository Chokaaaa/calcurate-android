package com.thecalcurate.android.ui

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.thecalcurate.android.databinding.CurrencyItemBinding
import com.thecalcurate.android.model.CurrencyItem


class CurrencyRecyclerViewAdapter internal constructor(
    context: Context?
) :
    RecyclerView.Adapter<CurrencyViewHolder>() {
    val TAG = "CurrencyRecyclerViewAdapter"
    private var mData: MutableList<CurrencyItem>? = null
    private val mInflater: LayoutInflater
    private var mClickListener: ItemClickListener? = null
    private var mOnClickListener: View.OnClickListener? = null

    // inflates the row layout from xml when needed
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CurrencyViewHolder {
//        Log.e(TAG, "onCreateViewHolder")
        val binding =
            CurrencyItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)

        return CurrencyViewHolder(binding, mClickListener, mOnClickListener)
    }

    // binds the data to the TextView in each row
    override fun onBindViewHolder(holder: CurrencyViewHolder, position: Int) {
        if (mData != null) {
            val item = mData?.get(position)
//            Log.e(TAG, "item?.name: ${item?.name}")
            holder.binding.root.tag = item
            holder.binding.item = item
            holder.binding.chbFav.tag = item?.code
        }
    }

    // total number of rows
    override fun getItemCount(): Int {
        return mData?.size ?: 0
    }

    // convenience method for getting data at click position
    fun getItem(id: Int): CurrencyItem? {
        return mData?.get(id)
    }

    // allows clicks events to be caught
    fun setClickListener(
        itemClickListener: ItemClickListener?,
        onClickListener: View.OnClickListener
    ) {
        mClickListener = itemClickListener
        mOnClickListener = onClickListener
    }

    fun setList(list: MutableList<CurrencyItem>) {
        mData = list
    }

    fun getList(): MutableList<CurrencyItem>? {
        return mData
    }

    // parent activity will implement this method to respond to click events
    interface ItemClickListener {
        fun onItemClick(view: View?, position: Int)
    }

    // data is passed into the constructor
    init {
        mInflater = LayoutInflater.from(context)
    }
}