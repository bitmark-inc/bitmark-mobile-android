package com.bitmark.registry.feature.property_detail

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bitmark.registry.R
import com.bitmark.registry.util.modelview.TransactionModelView
import kotlinx.android.synthetic.main.item_provenance.view.*


/**
 * @author Hieu Pham
 * @since 2019-07-15
 * Email: hieupham@bitmark.com
 * Copyright © 2019 Bitmark. All rights reserved.
 */
class ProvenanceRecyclerViewAdapter :
    RecyclerView.Adapter<ProvenanceRecyclerViewAdapter.ViewHolder>() {

    private val items = mutableListOf<Item>()

    fun add(accountNumber: String, provenances: List<TransactionModelView>) {
        val pos = this.items.size
        val items = provenances.map { p ->
            Item(
                p.confirmedAt(),
                p.owner,
                accountNumber,
                p.isPending()
            )
        }
        this.items.addAll(items)
        notifyItemRangeInserted(pos, items.size)
    }

    fun set(accountNumber: String, provenances: List<TransactionModelView>) {
        this.items.clear()
        val items = provenances.map { p ->
            Item(
                p.confirmedAt(),
                p.owner,
                accountNumber,
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
            )
        )

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) =
        holder.bind(items[position])


    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {

        fun bind(item: Item) {
            with(itemView) {
                if (item.isPending) {
                    val color = ContextCompat.getColor(context, R.color.silver)
                    tvConfirmedAt.setTextColor(color)
                    tvOwner.setTextColor(color)
                    tvConfirmedAt.text =
                        context.getString(R.string.wait_to_be_confirmed)
                } else {
                    val color =
                        ContextCompat.getColor(context, R.color.blue_ribbon)
                    tvConfirmedAt.setTextColor(color)
                    tvOwner.setTextColor(color)
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
            return String.format(
                "[%s...%s]",
                owner.substring(0, 4),
                owner.substring(len - 4, len)
            )
        }
    }
}