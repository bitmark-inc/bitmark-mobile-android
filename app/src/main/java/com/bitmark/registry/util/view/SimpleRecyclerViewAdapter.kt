/**
 * SPDX-License-Identifier: ISC
 * Copyright © 2014-2019 Bitmark. All rights reserved.
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 */
package com.bitmark.registry.util.view

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.DrawableRes
import androidx.annotation.LayoutRes
import androidx.recyclerview.widget.RecyclerView
import com.bitmark.registry.R
import kotlinx.android.synthetic.main.item_simple_recycler_view.view.*

class SimpleRecyclerViewAdapter(
    @LayoutRes private val layoutItemRes: Int = R.layout.item_simple_recycler_view,
    @DrawableRes private val itemBackground: Int? = null
) :
    RecyclerView.Adapter<SimpleRecyclerViewAdapter.ViewHolder>() {

    private val items = mutableListOf<String>()

    private var itemClickListener: ((String) -> Unit)? = null

    fun setItemClickListener(listener: (String) -> Unit) {
        this.itemClickListener = listener
    }

    fun set(items: List<String>, limit: Int = items.size) {
        this.items.clear()
        val canonicalLimit = if (limit > items.size) items.size else limit
        this.items.addAll(items.subList(0, canonicalLimit))
        notifyDataSetChanged()
    }

    fun add(items: List<String>) {
        val pos = this.items.size
        this.items.addAll(items)
        notifyItemRangeInserted(pos, items.size)
    }

    fun clear() {
        items.clear()
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ViewHolder = ViewHolder(
        LayoutInflater.from(parent.context).inflate(
            layoutItemRes,
            parent,
            false
        ), itemBackground, itemClickListener
    )

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }

    class ViewHolder(
        view: View, @DrawableRes private val itemBackground: Int?,
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
                if (itemBackground != null) tvContent.background =
                    context.getDrawable(itemBackground)
            }
        }
    }
}