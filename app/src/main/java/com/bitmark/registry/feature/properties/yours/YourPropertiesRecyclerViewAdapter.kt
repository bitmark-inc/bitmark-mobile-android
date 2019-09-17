package com.bitmark.registry.feature.properties.yours

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bitmark.registry.BuildConfig
import com.bitmark.registry.R
import com.bitmark.registry.data.model.AssetData
import com.bitmark.registry.data.model.BitmarkData
import com.bitmark.registry.util.extension.append
import com.bitmark.registry.util.extension.shortenAccountNumber
import com.bitmark.registry.util.modelview.BitmarkModelView
import com.bumptech.glide.Glide
import kotlinx.android.synthetic.main.item_your_properties.view.*
import java.io.File


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

    internal fun add(
        items: List<BitmarkModelView>
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

    internal fun remove(bitmarkId: String) {
        val pos = items.indexOfFirst { b -> b.id == bitmarkId }
        if (pos == -1) return
        items.removeAt(pos)
        notifyItemRemoved(pos)
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
        val settledItems = items.filter { i -> i.isSettled() }
            .sortedByDescending { t -> t.offset }
        items.clear()
        items.append(pendingItems, settledItems)
        notifyDataSetChanged()
    }

    internal fun updateAssetFile(assetId: String, file: File) {
        this.items.filter { i -> i.assetId == assetId }
            .forEach { i -> i.assetFile = file }
    }

    internal fun updateAssetType(assetId: String, type: AssetData.Type) {
        this.items.filter { i -> i.assetId == assetId }.forEach { i ->
            i.assetType = type
            notifyItemChanged(this.items.indexOf(i))
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

                tvName.text =
                    if (item.isMusicClaiming()) "${item.name} [${item.edition
                        ?: "?"}/${item.totalEdition ?: "?"}]" else item.name
                tvName.isSelected = true
                tvIssuer.text =
                    if (item.issuer == item.accountNumber) context.getString(R.string.you).toUpperCase() else item.readableIssuer
                        ?: item.issuer.shortenAccountNumber()
                tvConfirmedAt.text = when (item.status) {
                    BitmarkData.Status.ISSUING -> "${context.getString(R.string.registering).toUpperCase()}..."
                    BitmarkData.Status.TRANSFERRING -> "${context.getString(R.string.incoming).toUpperCase()}..."
                    BitmarkData.Status.SETTLED -> item.confirmedAt()?.toUpperCase()
                    else -> ""
                }

                if (item.isMusicClaiming()) {
                    val url =
                        "${BuildConfig.PROFILE_SERVER_ENDPOINT}/s/asset/thumbnail?asset_id=${item.assetId}"
                    Glide.with(context).load(url).into(ivAssetType)
                } else {
                    ivAssetType.setImageResource(item.getThumbnailRes())
                }

            }
        }

    }
}