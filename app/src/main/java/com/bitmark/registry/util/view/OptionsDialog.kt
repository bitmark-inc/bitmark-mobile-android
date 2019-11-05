/**
 * SPDX-License-Identifier: ISC
 * Copyright © 2014-2019 Bitmark. All rights reserved.
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 */
package com.bitmark.registry.util.view

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bitmark.registry.R
import com.bitmark.registry.util.extension.setTextColorRes
import kotlinx.android.synthetic.main.item_options.view.*
import kotlinx.android.synthetic.main.layout_options_bottom_sheet.*

class OptionsDialog(
    context: Context,
    private val title: String,
    private val cancelable: Boolean = true,
    private val items: List<OptionsAdapter.Item>,
    private val itemClickListener: ((OptionsAdapter.Item) -> Unit)
) : BaseBottomSheetDialog(context) {

    override fun layoutRes(): Int = R.layout.layout_options_bottom_sheet

    override fun initComponents() {
        super.initComponents()

        setCancelable(cancelable)

        tvTitle.text = title

        val layoutManager =
            LinearLayoutManager(context, RecyclerView.VERTICAL, false)
        rvOptions.layoutManager = layoutManager
        val adapter = OptionsAdapter()
        adapter.set(items)
        rvOptions.adapter = adapter

        adapter.setOnItemClickListener { item ->
            dismiss()
            itemClickListener.invoke(item)
        }
    }

    class OptionsAdapter : RecyclerView.Adapter<OptionsAdapter.ViewHolder>() {

        private val items = mutableListOf<Item>()

        private var onItemClickListener: ((Item) -> Unit)? = null

        fun setOnItemClickListener(listener: ((Item) -> Unit)?) {
            this.onItemClickListener = listener
        }

        fun set(items: List<Item>) {
            this.items.clear()
            this.items.addAll(items)
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(
            parent: ViewGroup,
            viewType: Int
        ): ViewHolder = ViewHolder(
            LayoutInflater.from(parent.context).inflate(
                R.layout.item_options,
                parent,
                false
            ), onItemClickListener
        )

        override fun getItemCount(): Int = items.size

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.bind(items[position])
        }

        class ViewHolder(v: View, clickListener: ((Item) -> Unit)?) :
            RecyclerView.ViewHolder(v) {

            private lateinit var item: Item

            init {
                itemView.setOnClickListener { clickListener?.invoke(item) }
            }

            fun bind(item: Item) {
                this.item = item
                with(itemView) {
                    ivIcon.setImageResource(item.icon)
                    tvOption.text = item.text
                    tvOption.setTextColorRes(item.textColor)
                }
            }
        }

        data class Item(
            @DrawableRes internal val icon: Int,
            internal val text: String,
            @ColorRes internal val textColor: Int = R.color.emperor
        )
    }
}