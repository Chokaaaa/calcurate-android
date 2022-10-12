package com.thecalcurate.android.ui

import android.view.View
import android.widget.CheckBox
import android.widget.CompoundButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.thecalcurate.android.R
import com.thecalcurate.android.databinding.CurrencyItemBinding

class CurrencyViewHolder internal constructor(
    val binding: CurrencyItemBinding,
    private val mClickListener: CurrencyRecyclerViewAdapter.ItemClickListener?,
    mOnClickListener: View.OnClickListener?
) : RecyclerView.ViewHolder(binding.root),
    View.OnClickListener {
//    var chbFav: CheckBox
//    var txvName: TextView
//    var txvRate: TextView

    override fun onClick(view: View?) {
        mClickListener?.onItemClick(view, adapterPosition)
    }

    init {
        itemView.findViewById<CheckBox>(R.id.chbFav).setOnClickListener(mOnClickListener)
//        txvName = itemView.findViewById(R.id.txvName)
//        txvRate = itemView.findViewById(R.id.txvRate)
        itemView.setOnClickListener(this)
    }
}