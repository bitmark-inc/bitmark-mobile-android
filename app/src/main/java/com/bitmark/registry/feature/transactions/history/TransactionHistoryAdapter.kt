/**
 * SPDX-License-Identifier: ISC
 * Copyright © 2014-2019 Bitmark. All rights reserved.
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 */
package com.bitmark.registry.feature.transactions.history

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bitmark.registry.R
import com.bitmark.registry.util.DateTimeUtil.Companion.ISO8601_FORMAT
import com.bitmark.registry.util.extension.*
import com.bitmark.registry.util.modelview.TransactionModelView
import kotlinx.android.synthetic.main.item_tx_history.view.*

class TransactionHistoryAdapter :
    RecyclerView.Adapter<TransactionHistoryAdapter.ViewHolder>() {

    private var itemClickListener: ((TransactionModelView) -> Unit)? = null

    private val items = mutableListOf<TransactionModelView>()

    internal fun setItemClickListener(listener: (TransactionModelView) -> Unit) {
        this.itemClickListener = listener
    }

    fun add(
        items: List<TransactionModelView>
    ) {
        // FIXME deduplicate items, need to be improved later on
        items.forEach { item ->
            val duplicatedItemIndex =
                this.items.indexOfFirst { i -> i.id == item.id }
            if (duplicatedItemIndex != -1) {
                this.items.removeAt(duplicatedItemIndex)
                notifyItemRemoved(duplicatedItemIndex)
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
            }
            this.items.add(i)
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

                tvName.text = tx.assetName
                tvName.isSelected = true

                if (tx.isAssetClaiming()) {

                    tvTxInfo.visible()
                    tvTxType.setText(R.string.claim_request)
                    tvSender.text =
                        context.getString(R.string.you).toUpperCase()
                    tvReceiver.text = tx.to?.shortenAccountNumber()

                    when {
                        tx.isAssetClaimingPending() -> {
                            ivStatus.gone()
                            tvTxInfo.setText(R.string.pending_three_dot)
                            tvConfirmedAt.setText(R.string.waiting_artist_confirm_three_dot)
                            tvTxInfo.setTextColorRes(R.color.dusty_gray_2)
                            tvConfirmedAt.setTextColorRes(R.color.dusty_gray_2)
                        }
                        tx.isAssetClaimingAccepted() -> {
                            ivStatus.visible()
                            ivStatus.setImageResource(R.drawable.ic_check)
                            tvConfirmedAt.text = tx.confirmedAt(ISO8601_FORMAT)
                            tvTxInfo.setTextColorRes(R.color.blue_ribbon)
                            tvConfirmedAt.setTextColorRes(R.color.blue_ribbon)
                        }
                        tx.isAssetClaimingRejected() -> {
                            ivStatus.visible()
                            ivStatus.setImageResource(R.drawable.ic_uncheck)
                            tvConfirmedAt.text = tx.confirmedAt(ISO8601_FORMAT)
                            tvTxInfo.setTextColorRes(R.color.torch_red)
                            tvConfirmedAt.setTextColorRes(R.color.torch_red)
                        }
                    }

                } else {

                    if (tx.isPending()) {
                        tvTxInfo.visible()
                        ivStatus.gone()
                        tvConfirmedAt.setTextColor(
                            ContextCompat.getColor(
                                context,
                                R.color.silver
                            )
                        )
                        tvConfirmedAt.setText(R.string.pending_three_dot)
                        tvTxInfo.setText(if (tx.isIssuance()) R.string.issuance else R.string.transfer)

                    } else {
                        tvTxInfo.gone()
                        ivStatus.visible()
                        ivStatus.setImageResource(R.drawable.ic_check)
                        tvConfirmedAt.setTextColor(
                            ContextCompat.getColor(
                                context,
                                R.color.blue_ribbon
                            )
                        )
                        tvConfirmedAt.text = tx.confirmedAt()
                    }

                    if (tx.isIssuance()) {
                        tvTxType.setText(R.string.property_issuance)
                        tvTo.invisible()
                        tvReceiver.invisible()
                        tvSender.text =
                            context.getString(R.string.you).toUpperCase()
                    } else {
                        tvTxType.setText(R.string.p2p_transfer)
                        tvTo.visible()
                        tvReceiver.visible()
                        if (tx.isOwning()) {
                            tvSender.text =
                                tx.previousOwner?.shortenAccountNumber() ?: ""
                            tvReceiver.text =
                                context.getString(R.string.you).toUpperCase()
                        } else {
                            tvReceiver.text =
                                tx.owner?.shortenAccountNumber() ?: ""
                            tvSender.text =
                                context.getString(R.string.you).toUpperCase()
                        }
                    }
                }
            }
        }
    }
}