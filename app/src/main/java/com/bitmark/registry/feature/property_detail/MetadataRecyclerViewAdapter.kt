package com.bitmark.registry.feature.property_detail

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.ColorRes
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bitmark.registry.R
import kotlinx.android.synthetic.main.item_metadata.view.*


/**
 * @author Hieu Pham
 * @since 2019-07-15
 * Email: hieupham@bitmark.com
 * Copyright © 2019 Bitmark. All rights reserved.
 */
class MetadataRecyclerViewAdapter(@ColorRes private var textColor: Int = android.R.color.black) :
    RecyclerView.Adapter<MetadataRecyclerViewAdapter.ViewHolder>() {

    private val items = mutableListOf<Pair<String, String>>()

    internal fun set(metadata: Map<String, String>) {
        items.clear()
        for (m in metadata) {
            items.add(Pair(m.key, m.value))
        }
        items.sortWith(Comparator { o1, o2 -> o1.first.compareTo(o2.first) })
        notifyDataSetChanged()
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

        internal fun bind(entry: Pair<String, String>, @ColorRes textColor: Int) {
            with(itemView) {
                val color = ContextCompat.getColor(context, textColor)
                tvKey.setTextColor(color)

                val key = entry.first
                val value = entry.second
                tvKey.text = key
                tvValue.text = value
            }
        }
    }
}