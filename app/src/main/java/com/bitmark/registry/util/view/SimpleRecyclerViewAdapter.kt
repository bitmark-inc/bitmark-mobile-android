package com.bitmark.registry.util.view

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.DrawableRes
import androidx.recyclerview.widget.RecyclerView
import com.bitmark.registry.R
import kotlinx.android.synthetic.main.item_simple_recycler_view.view.*


/**
 * @author Hieu Pham
 * @since 2019-08-01
 * Email: hieupham@bitmark.com
 * Copyright © 2019 Bitmark. All rights reserved.
 */
class SimpleRecyclerViewAdapter(@DrawableRes private val itemBackground: Int = R.drawable.bg_border_bottom_top_less_white_stateful) :
    RecyclerView.Adapter<SimpleRecyclerViewAdapter.ViewHolder>() {

    private val items = mutableListOf<String>()

    private var itemClickListener: ((String) -> Unit)? = null

    fun setItemClickListener(listener: (String) -> Unit) {
        this.itemClickListener = listener
    }

    fun add(items: List<String>) {
        val pos = this.items.size
        this.items.addAll(items)
        notifyItemRangeInserted(pos, items.size)
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ViewHolder = ViewHolder(
        LayoutInflater.from(parent.context).inflate(
            R.layout.item_simple_recycler_view,
            parent,
            false
        ), itemBackground, itemClickListener
    )

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }

    class ViewHolder(
        view: View, @DrawableRes private val itemBackground: Int,
        itemClickListener: ((String) -> Unit)?
    ) :
        RecyclerView.ViewHolder(view) {

        private lateinit var item: String

        init {
            with(itemView) {
                setOnClickListener { itemClickListener?.invoke(item) }
            }
        }

        fun bind(item: String) {
            this.item = item
            with(itemView) {
                tvContent.text = item
                tvContent.background = context.getDrawable(itemBackground)
            }
        }
    }
}