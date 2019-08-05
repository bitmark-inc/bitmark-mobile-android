package com.bitmark.registry.feature.issuance.issuance

import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.doOnTextChanged
import androidx.recyclerview.widget.RecyclerView
import com.bitmark.registry.R
import com.bitmark.registry.util.extension.gone
import com.bitmark.registry.util.extension.setBackgroundDrawable
import com.bitmark.registry.util.extension.setTextColorRes
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

    private val handler = Handler(Looper.getMainLooper())

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
        if (mapItem.isEmpty()) return
        items.clear()
        for (entry in mapItem.entries) {
            // do not display "source" key
            if (entry.key.equals("source", ignoreCase = true)) continue
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
            if (!i.isValid()) continue
            map[i.key.trim()] = i.value.trim()
        }
        return map
    }

    internal fun isValid() =
        (items.size == 1 && items[0].isBlank())
                || items.find { i -> !i.isValid() } == null
                && this.items.distinctBy { t -> t.key }.size == this.items.size

    internal fun disable() {
        items.forEach { i -> i.disable = true }
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ViewHolder = ViewHolder(
        LayoutInflater.from(parent.context).inflate(
            R.layout.item_metadata_input,
            parent,
            false
        ),
        internalItemDeletedListener,
        itemFilledListener
    )

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }

    inner class ViewHolder(
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

                if (item.disable) {
                    etKey.isFocusable = false
                    etValue.isFocusable = false
                    etKey.setTextColorRes(R.color.silver)
                    etValue.setTextColorRes(R.color.silver)
                } else {
                    etKey.isFocusableInTouchMode = true
                    etValue.isFocusableInTouchMode = true
                    etKey.setTextColorRes(android.R.color.black)
                    etValue.setTextColorRes(android.R.color.black)
                }

                etKey.setText(item.key.toUpperCase())
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
                    val duplicatedItem = items.find { i ->
                        i.key.isNotBlank() && i.key.equals(
                            text,
                            ignoreCase = true
                        )
                    }
                    item.duplicate =
                        duplicatedItem != null && item.hashCode() != duplicatedItem.hashCode()
                    item.key = text
                } else {
                    item.value = text
                }
                if (isDeleted && item.isMissing() || item.isProhibited() || item.duplicate) {
                    showInvalidState()
                } else if (item.isValid()) {
                    showValidState()
                }

                itemFilledListener?.invoke(item.isValid())
            }
        }

        private fun handleFocusState(`this`: View, hasFocus: Boolean) {
            with(itemView) {
                val that = if (`this` == etKey) etValue else etKey

                if (hasFocus && !item.isInvalidState()) {
                    // if it's currently missing, keep that state.
                    showValidState()
                } else {
                    // bit delay for waiting for ${that}'s focus event
                    handler.postDelayed(
                        {
                            if (!that.isFocused) {
                                if (!item.isValid()) {
                                    showInvalidState()
                                }
                            }

                        },
                        100
                    )
                }
            }
        }

        private fun showInvalidState() {
            item.state = Item.State.INVALID
            with(itemView) {
                if (!item.disable) {
                    etKey.setBackgroundDrawable(R.drawable.bg_border_torch_red)
                    etValue.setBackgroundDrawable(R.drawable.bg_border_torch_red_top_less)
                } else {
                    etKey.setBackgroundDrawable(R.drawable.bg_border_silver)
                    etValue.setBackgroundDrawable(R.drawable.bg_border_silver_top_less)
                }
            }
        }

        private fun showValidState() {
            item.state = Item.State.VALID
            with(itemView) {
                if (!item.disable) {
                    etKey.setBackgroundDrawable(R.drawable.bg_border_blue_ribbon)
                    etValue.setBackgroundDrawable(R.drawable.bg_border_blue_ribbon_top_less)
                } else {
                    etKey.setBackgroundDrawable(R.drawable.bg_border_silver)
                    etValue.setBackgroundDrawable(R.drawable.bg_border_silver_top_less)
                }
            }
        }

    }

    data class Item(
        var key: String,
        var value: String,
        var removable: Boolean = false,
        var disable: Boolean = false,
        var duplicate: Boolean = false,
        var state: State = State.INITIALIZE
    ) {

        enum class State {
            VALID, INVALID, INITIALIZE
        }

        fun isInvalidState() = state == State.INVALID

        fun isProhibited() = key.equals(
            "source",
            ignoreCase = true
        )

        fun isBlank() = key.isBlank() && value.isBlank()

        fun isMissing() = key.isBlank() || value.isBlank()

        fun isValid() = !isMissing() && !isProhibited() && !duplicate
    }
}
