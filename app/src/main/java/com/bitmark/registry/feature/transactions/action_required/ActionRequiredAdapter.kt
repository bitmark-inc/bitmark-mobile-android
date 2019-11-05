/**
 * SPDX-License-Identifier: ISC
 * Copyright © 2014-2019 Bitmark. All rights reserved.
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 */
package com.bitmark.registry.feature.transactions.action_required

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bitmark.registry.R
import com.bitmark.registry.data.model.entity.ActionRequired
import com.bitmark.registry.util.extension.getString
import com.bitmark.registry.util.modelview.ActionRequiredModelView
import kotlinx.android.synthetic.main.item_action_required.view.*

class ActionRequiredAdapter :
    RecyclerView.Adapter<ActionRequiredAdapter.ViewHolder>() {

    private var clickListener: ((ActionRequiredModelView) -> Unit)? = null

    private val items = mutableListOf<ActionRequiredModelView>()

    fun setItemClickListener(clickListener: ((ActionRequiredModelView) -> Unit)?) {
        this.clickListener = clickListener
    }

    fun add(actions: List<ActionRequiredModelView>) {
        val pos = items.size
        items.addAll(actions)
        notifyItemRangeInserted(pos, actions.size)
    }

    fun remove(id: ActionRequired.Id) {
        val pos = items.indexOfFirst { i -> i.id == id }
        if (pos != -1) {
            items.removeAt(pos)
            notifyItemRemoved(pos)
        }
    }

    fun isEmpty() = items.isEmpty()

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ViewHolder = ViewHolder(
        LayoutInflater.from(parent.context).inflate(
            R.layout.item_action_required,
            parent,
            false
        ), clickListener
    )

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }

    class ViewHolder(
        view: View,
        clickListener: ((ActionRequiredModelView) -> Unit)?
    ) :
        RecyclerView.ViewHolder(view) {

        private lateinit var action: ActionRequiredModelView

        init {
            with(itemView) {
                setOnClickListener { clickListener?.invoke(action) }
            }
        }

        fun bind(action: ActionRequiredModelView) {
            this.action = action
            with(itemView) {
                when (action.type) {
                    ActionRequired.Type.SECURITY_ALERT -> tvType.setText(R.string.security_alert)
                }

                tvTitle.text =
                    context.getString(action.titleStringResName)
                tvDescription.text =
                    context.getString(action.descriptionStringResName)
                tvDate.text = action.getShortenDate()
            }
        }
    }
}
