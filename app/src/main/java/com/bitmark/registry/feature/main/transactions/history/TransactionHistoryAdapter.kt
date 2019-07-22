package com.bitmark.registry.feature.main.transactions.history

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bitmark.registry.R
import com.bitmark.registry.util.extension.gone
import com.bitmark.registry.util.extension.invisible
import com.bitmark.registry.util.extension.shortenAccountNumber
import com.bitmark.registry.util.extension.visible
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

    fun add(items: List<TransactionModelView>) {
        val pos = this.items.size
        this.items.addAll(items)
        notifyItemRangeInserted(pos, items.size)
    }

    fun clear() {
        items.clear()
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