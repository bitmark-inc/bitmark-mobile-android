package com.bitmark.registry.feature.property_detail

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.ColorInt
import androidx.annotation.ColorRes
import androidx.recyclerview.widget.RecyclerView
import com.bitmark.registry.R
import kotlinx.android.synthetic.main.item_metadata.view.*


/**
 * @author Hieu Pham
 * @since 2019-07-15
 * Email: hieupham@bitmark.com
 * Copyright © 2019 Bitmark. All rights reserved.
 */
class MetadataRecyclerViewAdapter(@ColorInt private var textColor: Int) :
    RecyclerView.Adapter<MetadataRecyclerViewAdapter.ViewHolder>() {

    private val items = mutableListOf<Pair<String, String>>()

    internal fun add(metadata: Map<String, String>) {
        val pos = items.size
        for (m in metadata) {
            items.add(Pair(m.key, m.value))
        }
        notifyItemRangeInserted(pos, pos + metadata.size)
    }

    internal fun changeTextColor(@ColorRes color: Int) {
        textColor = color
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ViewHolder = ViewHolder(
        LayoutInflater.from(parent.context).inflate(
            R.layout.item_metadata,
            parent,
            false
        )
    )

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) =
        holder.bind(items[position], textColor)

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {

        internal fun bind(entry: Pair<String, String>, @ColorInt textColor: Int) {
            with(itemView) {
                tvKey.setTextColor(textColor)

                val key = entry.first
                val value = entry.second
                tvKey.text = if (key.length > 15) key.substring(
                    0,
                    15
                ).toUpperCase() + "...:" else key.toUpperCase() + ":" // max 15 characters for better ui
                tvValue.text = value
            }
        }
    }
}