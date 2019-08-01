package com.bitmark.registry.feature.issuance.issuance

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.doOnTextChanged
import androidx.recyclerview.widget.RecyclerView
import com.bitmark.registry.R
import com.bitmark.registry.util.extension.gone
import com.bitmark.registry.util.extension.setBackgroundDrawable
import com.bitmark.registry.util.extension.visible
import kotlinx.android.synthetic.main.item_metadata_input.view.*


/**
 * @author Hieu Pham
 * @since 2019-07-31
 * Email: hieupham@bitmark.com
 * Copyright © 2019 Bitmark. All rights reserved.
 */
class MetadataRecyclerViewAdapter :
    RecyclerView.Adapter<MetadataRecyclerViewAdapter.ViewHolder>() {

    private val items = mutableListOf<Item>()

    private var itemFilledListener: ((Boolean) -> Unit)? = null

    private val internalItemDeletedListener: (Item) -> Unit = { item ->
        val pos = items.indexOf(item)
        if (pos != -1) {
            items.removeAt(pos)
            notifyItemRemoved(pos)
            changeRemovableState(isRemovable())
        }
    }

    init {
        // always keep 1 items at first
        add()
    }

    internal fun setItemFilledListener(listener: (Boolean) -> Unit) {
        this.itemFilledListener = listener
    }

    internal fun add() {
        val pos = items.size
        items.add(Item("", ""))
        notifyItemInserted(pos)
    }

    internal fun set(mapItem: Map<String, String>) {
        if(mapItem.isEmpty()) return
        items.clear()
        for (entry in mapItem.entries) {
            items.add(Item(entry.key, entry.value))
        }
        notifyDataSetChanged()
    }

    internal fun changeRemovableState(removable: Boolean) {
        this.items.forEach { item ->
            item.removable = removable
        }
        notifyDataSetChanged()
    }

    internal fun isRemovable() = items.size > 1

    internal fun toMap(): Map<String, String> {
        val map = mutableMapOf<String, String>()
        for (i in items) {
            if (i.isInvalid()) continue
            map[i.key] = i.value
        }
        return map
    }

    internal fun isFilled() = items.find { i -> i.isInvalid() } == null

    internal fun isValid() =
        (items.size == 1 && items[0].isBlank()) || isFilled()

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ViewHolder = ViewHolder(
        LayoutInflater.from(parent.context).inflate(
            R.layout.item_metadata_input,
            parent,
            false
        ), internalItemDeletedListener, itemFilledListener
    )

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }

    class ViewHolder(
        view: View,
        deleteClickListener: ((Item) -> Unit)?,
        private val itemFilledListener: ((Boolean) -> Unit)?
    ) :
        RecyclerView.ViewHolder(view) {

        private lateinit var item: Item

        init {
            with(itemView) {
                etKey.setOnFocusChangeListener { view, hasFocus ->
                    handleFocusState(view, hasFocus)
                }

                etValue.setOnFocusChangeListener { view, hasFocus ->
                    handleFocusState(view, hasFocus)
                }

                etKey.doOnTextChanged { text, _, _, _ ->
                    handleTextChanged(etKey, text.toString())
                }

                etValue.doOnTextChanged { text, _, _, _ ->
                    handleTextChanged(etValue, text.toString())
                }

                ivDelete.setOnClickListener {
                    deleteClickListener?.invoke(item)
                }
            }
        }

        fun bind(item: Item) {
            this.item = item
            with(itemView) {
                if (item.removable) {
                    ivDelete.visible()
                } else {
                    ivDelete.gone()
                }
                etKey.setText(item.key)
                etValue.setText(item.value)
            }
        }

        private fun handleTextChanged(
            view: View,
            text: String
        ) {
            with(itemView) {
                val isDeleted =
                    text.isBlank() && (view == etKey && item.key.isNotBlank()) || (view == etValue && item.value.isNotBlank())
                if (view == etKey) {
                    item.key = text
                } else {
                    item.value = text
                }
                if (isDeleted && item.isInvalid()) {
                    showMissing()
                } else if (!item.isInvalid()) {
                    showFilled()
                }

                itemFilledListener?.invoke(!item.isInvalid())
            }
        }

        private fun handleFocusState(`this`: View, hasFocus: Boolean) {
            with(itemView) {
                val that = if (`this` == etKey) etValue else etKey

                if (hasFocus && !item.isMissing()) {
                    // if it's currently missing, keep that state.
                    showFilled()
                } else {
                    // bit delay for waiting for ${that}'s focus event
                    handler.postDelayed(
                        {
                            if (!that.isFocused) {
                                if (item.isInvalid()) {
                                    showMissing()
                                }
                            }

                        },
                        100
                    )
                }
            }
        }

        private fun showMissing() {
            item.state = Item.State.MISSING
            with(itemView) {
                etKey.setBackgroundDrawable(R.drawable.bg_border_torch_red)
                etValue.setBackgroundDrawable(R.drawable.bg_border_torch_red_top_less)
            }
        }

        private fun showFilled() {
            item.state = Item.State.FILLED
            with(itemView) {
                etKey.setBackgroundDrawable(R.drawable.bg_border_blue_ribbon)
                etValue.setBackgroundDrawable(R.drawable.bg_border_blue_ribbon_top_less)
            }
        }

    }

    data class Item(
        var key: String,
        var value: String,
        var removable: Boolean = false,
        var state: State = State.INITIALIZE
    ) {

        enum class State {
            FILLED, MISSING, INITIALIZE
        }

        fun isMissing() = state == State.MISSING

        fun isInvalid() = key.isBlank() || value.isBlank()

        fun isBlank() = key.isBlank() && value.isBlank()
    }
}
