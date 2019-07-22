package com.bitmark.registry.feature.main.properties.yours

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bitmark.registry.R
import com.bitmark.registry.data.model.BitmarkData
import com.bitmark.registry.util.extension.shortenAccountNumber
import com.bitmark.registry.util.modelview.BitmarkModelView
import com.bitmark.registry.util.modelview.BitmarkModelView.AssetType.*
import kotlinx.android.synthetic.main.item_your_properties.view.*


/**
 * @author Hieu Pham
 * @since 2019-07-10
 * Email: hieupham@bitmark.com
 * Copyright © 2019 Bitmark. All rights reserved.
 */
class YourPropertiesRecyclerViewAdapter() :
    RecyclerView.Adapter<YourPropertiesRecyclerViewAdapter.ViewHolder>() {

    private val items = mutableListOf<BitmarkModelView>()

    private var itemClickListener: ((BitmarkModelView) -> Unit)? = null

    internal fun add(items: List<BitmarkModelView>) {
        val pos = this.items.size
        this.items.addAll(items)
        notifyItemRangeInserted(pos, items.size)
    }

    internal fun remove(bitmarkIds: List<String>) {
        if (bitmarkIds.isNullOrEmpty()) return
        val pos = bitmarkIds.map { b -> items.indexOfFirst { i -> i.id == b } }
        if (pos.isNullOrEmpty()) return
        pos.forEach { p ->
            if (p == -1) return@forEach
            items.removeAt(p)
            notifyItemRemoved(p)
        }
    }

    internal fun set(items: List<BitmarkModelView>) {
        this.items.clear()
        this.items.addAll(items)
        notifyDataSetChanged()
    }

    internal fun clear() {
        this.items.clear()
        notifyDataSetChanged()
    }

    internal fun update(items: List<BitmarkModelView>) {
        items.forEach { i ->
            val index = this.items.indexOfFirst { b -> b.id == i.id }
            if (index == -1) return
            this.items.removeAt(index)
            this.items.add(index, i)
            notifyItemChanged(index)
        }
    }

    internal fun setOnItemClickListener(clickListener: (BitmarkModelView) -> Unit) {
        this.itemClickListener = clickListener
    }

    internal fun markSeen(bitmarkId: String) {
        val item = this.items.find { b -> b.id == bitmarkId }
        if (item != null) {
            val pos = this.items.indexOf(item)
            item.seen = true
            notifyItemChanged(pos)
        }

    }

    internal fun isEmpty() = items.isEmpty()

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_your_properties, parent, false)
        return ViewHolder(view, itemClickListener)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }

    class ViewHolder(
        view: View,
        itemClickListener: ((BitmarkModelView) -> Unit)?
    ) :
        RecyclerView.ViewHolder(view) {

        private lateinit var item: BitmarkModelView

        init {
            itemView.setOnClickListener { itemClickListener?.invoke(item) }
        }

        fun bind(item: BitmarkModelView) {
            this.item = item
            with(itemView) {
                if (item.seen) {
                    tvConfirmedAt.setTextColor(
                        ContextCompat.getColor(
                            context,
                            android.R.color.black
                        )
                    )
                    tvIssuer.setTextColor(
                        ContextCompat.getColor(
                            context,
                            android.R.color.black
                        )
                    )
                    tvName.setTextColor(
                        ContextCompat.getColor(
                            context,
                            android.R.color.black
                        )
                    )
                } else {
                    tvConfirmedAt.setTextColor(
                        ContextCompat.getColor(
                            context,
                            R.color.blue_ribbon
                        )
                    )
                    tvIssuer.setTextColor(
                        ContextCompat.getColor(
                            context,
                            R.color.blue_ribbon
                        )
                    )
                    tvName.setTextColor(
                        ContextCompat.getColor(
                            context,
                            R.color.blue_ribbon
                        )
                    )
                }

                tvName.text = item.name
                tvIssuer.text =
                    if (item.issuer == item.accountNumber) context.getString(R.string.you).toUpperCase() else item.issuer.shortenAccountNumber()
                tvConfirmedAt.text = when (item.status) {
                    BitmarkData.Status.ISSUING -> context.getString(R.string.registering).toUpperCase()
                    BitmarkData.Status.TRANSFERRING -> context.getString(R.string.incoming).toUpperCase()
                    BitmarkData.Status.SETTLED -> item.confirmedAt()?.toUpperCase()
                    else -> ""
                }
                ivAssetType.setImageResource(
                    when (item.assetType) {
                        IMAGE -> R.drawable.ic_asset_image
                        VIDEO -> R.drawable.ic_asset_video
                        HEALTH -> R.drawable.ic_asset_health_data
                        MEDICAL -> R.drawable.ic_asset_medical_record
                        ZIP -> R.drawable.ic_asset_zip
                        DOC -> R.drawable.ic_asset_doc
                        UNKNOWN -> R.drawable.ic_asset_unknow
                    }
                )
            }
        }

    }
}