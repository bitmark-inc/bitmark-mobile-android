package com.bitmark.registry.feature.issuance.issuance

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.EditText
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

    private var deleteClickListener: ((Item) -> Unit)? = null

    private var itemFocusChangedListener: ((Int, Boolean) -> Unit)? = null

    private var actionDoneClickListener: ((Boolean) -> Unit)? = null

    init {
        // always keep 1 items at first
        add()
    }

    internal fun setItemFilledListener(listener: (Boolean) -> Unit) {
        this.itemFilledListener = listener
    }

    internal fun setItemDeleteClickListener(listener: (Item) -> Unit) {
        this.deleteClickListener = listener
    }

    internal fun setItemFocusChangedListener(listener: (Int, Boolean) -> Unit) {
        this.itemFocusChangedListener = listener
    }

    internal fun setActionDoneClickListener(listener: (Boolean) -> Unit) {
        this.actionDoneClickListener = listener
    }

    internal fun add(requestFocus: Boolean = false) {
        val pos = items.size
        items.add(Item("", "", isFocused = requestFocus))
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
            if (!i.isValid() || !i.isFilled()) continue
            map[i.key.trim()] = i.value.trim()
        }
        return map
    }

    internal fun isValid() = hasSingleBlankRow() || hasValidRows()

    internal fun hasSingleRow() = items.size == 1

    internal fun hasSingleBlankRow() = hasSingleRow() && items[0].isBlank()

    internal fun hasValidRows() =
        items.find { i -> !i.isValid() } == null && this.items.distinctBy { t -> t.key }.size == this.items.size

    internal fun hasBlankRow() = items.find { i -> i.isBlank() } != null

    internal fun isRemoving() = items.indexOfFirst { i -> i.removable } != -1

    internal fun disable() {
        items.forEach { i -> i.disable = true }
        notifyDataSetChanged()
    }

    internal fun remove(item: Item) {
        val index = items.indexOf(item)
        if (index == -1) return
        items.removeAt(index)
        notifyItemRemoved(index)
    }

    internal fun clear(pos: Int) {
        val item = items[pos]
        item.key = ""
        item.value = ""
        notifyItemChanged(pos)
    }

    internal fun requestNextFocus() {
        val focusedIndex = items.indexOfFirst { i -> i.isFocused }
        if (focusedIndex == items.size - 1) return
        val nextFocusIndex = focusedIndex + 1
        items[nextFocusIndex].isFocused = true
        notifyItemChanged(nextFocusIndex)
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
        deleteClickListener,
        itemFilledListener,
        itemFocusChangedListener,
        actionDoneClickListener
    )

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }

    inner class ViewHolder(
        view: View,
        deleteClickListener: ((Item) -> Unit)?,
        private val itemFilledListener: ((Boolean) -> Unit)?,
        private val itemFocusChangedListener: ((Int, Boolean) -> Unit)?,
        private val actionDoneClickListener: ((Boolean) -> Unit)?
    ) :
        RecyclerView.ViewHolder(view) {

        private lateinit var item: Item

        init {
            with(itemView) {
                etKey.setOnFocusChangeListener { view, hasFocus ->
                    itemFocusChangedListener?.let {
                        it(
                            adapterPosition,
                            hasFocus
                        )
                    }
                    handleFocusState(view, hasFocus)
                }

                etValue.setOnFocusChangeListener { view, hasFocus ->
                    itemFocusChangedListener?.let {
                        it(
                            adapterPosition,
                            hasFocus
                        )
                    }
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

                etKey.setOnEditorActionListener { view, actionId, event ->
                    if (event == null) {
                        handleEditorAction(view, actionId)
                    } else {
                        false
                    }
                }

                etValue.setOnEditorActionListener { view, actionId, event ->
                    if (event == null) {
                        handleEditorAction(view, actionId)
                    } else {
                        false
                    }
                }
            }
        }

        private fun handleEditorAction(view: View, actionId: Int) =
            when (actionId) {
                EditorInfo.IME_ACTION_DONE -> {
                    actionDoneClickListener?.let { it(isLastItem()) }
                    true
                }

                EditorInfo.IME_ACTION_NEXT -> {
                    if (view == itemView.etKey) {
                        // workaround to force the etKey get ime DONE at the first time get focus
                        itemView.etValue.imeOptions = if (isLastItem()) {
                            EditorInfo.IME_ACTION_DONE
                        } else {
                            EditorInfo.IME_ACTION_UNSPECIFIED
                        }
                    } else {
                        requestNextFocus()
                    }
                    false
                }

                else -> false
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
                etKey.setSelection(item.key.length)
                etValue.setSelection(item.value.length)

                if (item.isFocused) {
                    if (item.isBlank()) {
                        etKey.requestFocus()
                    }
                } else {
                    etKey.clearFocus()
                    etValue.clearFocus()
                }
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

                if (item.isBlank()) {
                    showInitState()
                } else if ((isDeleted && item.isMissing()) || item.isProhibited() || item.duplicate) {
                    showInvalidState()
                } else {
                    showValidState()
                }

                itemFilledListener?.invoke(item.isFilled())
            }
        }

        private fun handleFocusState(`this`: View, hasFocus: Boolean) {
            with(itemView) {
                val that = if (`this` == etKey) etValue else etKey

                if (hasFocus) {
                    // if it's currently missing, keep that state.
                    if (!item.isInvalidState()) {
                        showValidState()
                    }
                    (`this` as EditText).imeOptions =
                        if (`this` == etValue && isLastItem()) {
                            EditorInfo.IME_ACTION_DONE
                        } else {
                            EditorInfo.IME_ACTION_UNSPECIFIED
                        }
                    item.isFocused = true
                } else {
                    // bit delay for waiting for ${that}'s focus event
                    handler.postDelayed(
                        {
                            if (!that.isFocused) {
                                item.isFocused = false
                                if (item.isMissing() || item.isProhibited() || item.duplicate) {
                                    showInvalidState()
                                } else if (item.isBlank()) {
                                    showInitState()
                                }
                            }

                        },
                        100
                    )
                }
            }
        }

        private fun isLastItem() = adapterPosition == items.size - 1

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

        private fun showInitState() {
            item.state = Item.State.INITIALIZE
            with(itemView) {
                etKey.setBackgroundDrawable(R.drawable.bg_border_silver)
                etValue.setBackgroundDrawable(R.drawable.bg_border_silver_top_less)
            }
        }

    }

    data class Item(
        var key: String,
        var value: String,
        var removable: Boolean = false,
        var disable: Boolean = false,
        var duplicate: Boolean = false,
        var isFocused: Boolean = false,
        var state: State = State.INITIALIZE
    ) {

        enum class State {
            VALID,
            INVALID,
            INITIALIZE
        }

        fun isInvalidState() = state == State.INVALID

        fun isProhibited() = key.equals(
            "source",
            ignoreCase = true
        )

        fun isBlank() = key.isBlank() && value.isBlank()

        fun isFilled() = !isMissing() && !isBlank()

        fun isMissing() =
            (key.isBlank() && !value.isBlank()) || (!key.isBlank() && value.isBlank())

        fun isValid() = !isMissing() && !isProhibited() && !duplicate
    }
}
