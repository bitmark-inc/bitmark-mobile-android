/**
 * SPDX-License-Identifier: ISC
 * Copyright © 2014-2019 Bitmark. All rights reserved.
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 */
package com.bitmark.registry.feature.property_detail

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bitmark.registry.R
import com.bitmark.registry.util.extension.setSafetyOnclickListener
import com.bitmark.registry.util.modelview.TransactionModelView
import kotlinx.android.synthetic.main.item_provenance.view.*

class ProvenanceRecyclerViewAdapter :
    RecyclerView.Adapter<ProvenanceRecyclerViewAdapter.ViewHolder>() {

    private val items = mutableListOf<Item>()

    private var itemClickListener: ((Item) -> Unit)? = null

    internal fun setItemClickListener(listener: (Item) -> Unit) {
        this.itemClickListener = listener
    }

    fun add(accountNumber: String, provenances: List<TransactionModelView>) {
        val pos = this.items.size
        val items = provenances.map { p ->
            Item(
                p.confirmedAt(),
                p.owner!!,
                accountNumber,
                p.isPending()
            )
        }
        this.items.addAll(items)
        notifyItemRangeInserted(pos, items.size)
    }

    fun set(provenances: List<TransactionModelView>) {
        this.items.clear()
        val items = provenances.map { p ->
            Item(
                p.confirmedAt(),
                p.owner!!,
                p.accountNumber!!,
                p.isPending()
            )
        }
        this.items.addAll(items)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ViewHolder =
        ViewHolder(
            LayoutInflater.from(parent.context).inflate(
                R.layout.item_provenance,
                parent,
                false
            ), itemClickListener
        )

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) =
        holder.bind(items[position])


    class ViewHolder(view: View, itemClickListener: ((Item) -> Unit)?) :
        RecyclerView.ViewHolder(view) {

        private lateinit var item: Item

        init {
            with(itemView) {
                setSafetyOnclickListener { itemClickListener?.invoke(item) }
            }
        }

        fun bind(item: Item) {
            this.item = item
            with(itemView) {
                if (item.isPending) {
                    val color = ContextCompat.getColor(context, R.color.silver)
                    tvConfirmedAt.setTextColor(color)
                    tvOwner.setTextColor(color)
                    tvConfirmedAt.text =
                        context.getString(R.string.pending)
                } else {
                    tvConfirmedAt.setTextColor(
                        ContextCompat.getColor(
                            context,
                            android.R.color.black
                        )
                    )
                    tvOwner.setTextColor(
                        ContextCompat.getColor(
                            context,
                            R.color.blue_ribbon
                        )
                    )
                    tvConfirmedAt.text = item.confirmedAt
                }
                tvOwner.text =
                    if (item.accountNumber == item.owner) context.getString(
                        R.string.you
                    ).toUpperCase() else item.getShortenOwner()

            }
        }
    }

    data class Item(
        internal val confirmedAt: String,
        internal val owner: String,
        internal val accountNumber: String,
        internal val isPending: Boolean = false
    ) {
        fun getShortenOwner(): String {
            val len = owner.length
            return "[%s...%s]".format(
                owner.substring(0, 4),
                owner.substring(len - 4, len)
            )
        }
    }
}