package com.bitmark.registry.feature.transactions.history

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bitmark.registry.R
import com.bitmark.registry.util.extension.*
import com.bitmark.registry.util.modelview.TransactionModelView
import kotlinx.android.synthetic.main.item_tx_history.view.*


/**
 * @author Hieu Pham
 * @since 2019-07-21
 * Email: hieupham@bitmark.com
 * Copyright © 2019 Bitmark. All rights reserved.
 */
class TransactionHistoryAdapter :
    RecyclerView.Adapter<TransactionHistoryAdapter.ViewHolder>() {

    private var itemClickListener: ((TransactionModelView) -> Unit)? = null

    private val items = mutableListOf<TransactionModelView>()

    internal fun setItemClickListener(listener: (TransactionModelView) -> Unit) {
        this.itemClickListener = listener
    }

    fun add(
        items: List<TransactionModelView>,
        needDeduplication: Boolean = false
    ) {
        if (needDeduplication) {
            items.forEach { item ->
                val duplicatedItemIndex =
                    this.items.indexOfFirst { i -> i.id == item.id }
                if (duplicatedItemIndex != -1) {
                    this.items.removeAt(duplicatedItemIndex)
                    notifyItemRemoved(duplicatedItemIndex)
                }
            }
        }
        val pos = this.items.size
        this.items.addAll(items)
        notifyItemRangeInserted(pos, items.size)
    }

    fun clear() {
        items.clear()
        notifyDataSetChanged()
    }

    internal fun update(items: List<TransactionModelView>) {
        if (items.isEmpty()) return
        items.forEach { i ->
            val index = this.items.indexOfFirst { item -> item.id == i.id }
            if (index != -1) {
                this.items.removeAt(index)
                this.items.add(index, i)
            } else {
                this.items.add(i)
            }
        }
        // FIXME bad solution to avoid wrong order
        reorder()
    }

    private fun reorder() {
        val pendingItems = items.filter { i -> i.isPending() }
            .sortedByDescending { t -> t.offset }
        val confirmedItems = items.filter { i -> !i.isPending() }
            .sortedByDescending { t -> t.offset }
        items.clear()
        items.append(pendingItems, confirmedItems)
        notifyDataSetChanged()
    }

    fun isEmpty() = items.isEmpty()

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ViewHolder = ViewHolder(
        LayoutInflater.from(parent.context).inflate(
            R.layout.item_tx_history,
            parent,
            false
        ), itemClickListener
    )

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }

    class ViewHolder(
        view: View,
        private val clickListener: ((TransactionModelView) -> Unit)?
    ) : RecyclerView.ViewHolder(view) {

        private lateinit var tx: TransactionModelView

        init {
            with(itemView) {
                layoutRoot.setOnClickListener { clickListener?.invoke(tx) }
            }
        }

        fun bind(tx: TransactionModelView) {
            this.tx = tx
            with(itemView) {
                if (tx.isPending()) {
                    tvTxTypeShort.visible()
                    ivConfirmed.gone()
                    tvConfirmedAt.setTextColor(
                        ContextCompat.getColor(
                            context,
                            R.color.silver
                        )
                    )
                    tvConfirmedAt.text =
                        "%s...".format(context.getString(R.string.pending))
                    tvTxTypeShort.setText(if (tx.isIssuance()) R.string.issuance else R.string.transfer)

                } else {
                    tvTxTypeShort.gone()
                    ivConfirmed.visible()
                    tvConfirmedAt.setTextColor(
                        ContextCompat.getColor(
                            context,
                            R.color.blue_ribbon
                        )
                    )
                    tvConfirmedAt.text = tx.confirmedAt()
                }

                tvName.text = tx.assetName

                if (tx.isIssuance()) {
                    tvTxType.text = context.getString(R.string.issuance)
                    tvTo.invisible()
                    tvReceiver.invisible()
                    tvSender.text =
                        context.getString(R.string.you).toUpperCase()
                } else {
                    tvTxType.text = context.getString(R.string.p2p_transfer)
                    tvTo.visible()
                    tvReceiver.visible()
                    if (tx.isOwning()) {
                        tvSender.text =
                            tx.previousOwner?.shortenAccountNumber() ?: ""
                        tvReceiver.text =
                            context.getString(R.string.you).toUpperCase()
                    } else {
                        tvReceiver.text =
                            tx.previousOwner?.shortenAccountNumber() ?: ""
                        tvSender.text =
                            context.getString(R.string.you).toUpperCase()
                    }
                }
            }
        }
    }
}