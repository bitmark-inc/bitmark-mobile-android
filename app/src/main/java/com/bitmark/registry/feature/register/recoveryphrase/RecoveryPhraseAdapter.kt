package com.bitmark.registry.feature.register.recoveryphrase

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.annotation.ColorRes
import androidx.core.content.ContextCompat
import androidx.core.widget.doAfterTextChanged
import androidx.core.widget.doOnTextChanged
import androidx.recyclerview.widget.RecyclerView
import com.bitmark.registry.R
import kotlinx.android.synthetic.main.item_recovery_phrase.view.*


/**
 * @author Hieu Pham
 * @since 2019-07-08
 * Email: hieupham@bitmark.com
 * Copyright © 2019 Bitmark. All rights reserved.
 */
class RecoveryPhraseAdapter(
    private val editable: Boolean = true,
    @ColorRes private val textColor: Int = R.color.gray
) : RecyclerView.Adapter<RecoveryPhraseAdapter.ViewHolder>(),
    OnTextChangeListener {

    private val items = mutableListOf<Item>()

    private var onTextChangeListener: OnTextChangeListener? = null

    private var onDoneListener: (() -> Unit)? = null

    private var onItemClickListener: ((Item) -> Unit)? = null

    private val itemClickListener: (Int) -> Unit = { pos ->
        onItemClickListener?.invoke(items[pos])
    }

    fun setOnTextChangeListener(listener: OnTextChangeListener?) {
        this.onTextChangeListener = listener
    }

    fun setOnDoneListener(listener: () -> Unit) {
        this.onDoneListener = listener
    }

    fun setOnItemClickListener(listener: (Item) -> Unit) {
        this.onItemClickListener = listener
    }

    internal fun requestNextFocus(): Boolean {
        val focusedIndex = items.indexOfFirst { i -> i.focused }
        if (focusedIndex != -1) {
            items[focusedIndex].focused = false
            notifyItemChanged(focusedIndex)
            val nextFocusIndex =
                items.indexOfFirst { i -> i.sequence == items[focusedIndex].sequence + 1 }
            if (nextFocusIndex != -1) {
                items[nextFocusIndex].focused = true
                notifyItemChanged(nextFocusIndex)
                return true
            }
        }
        return false
    }

    internal fun requestPrevFocus(): Boolean {
        val focusedIndex = items.indexOfFirst { i -> i.focused }
        if (focusedIndex != -1) {
            items[focusedIndex].focused = false
            notifyItemChanged(focusedIndex)
            val prevFocusIndex =
                items.indexOfFirst { i -> i.sequence == items[focusedIndex].sequence - 1 }
            if (prevFocusIndex != -1) {
                items[prevFocusIndex].focused = true
                notifyItemChanged(prevFocusIndex)
                return true
            }
        }
        return false
    }

    internal fun clearFocus() {
        val focusedIndex = items.indexOfFirst { i -> i.focused }
        if (focusedIndex == -1) return
        items[focusedIndex].focused = false
        notifyItemChanged(focusedIndex)
    }

    fun setDefault(version: Version = Version.TWELVE) {
        val length = version.value
        if (length % 2 != 0) throw RuntimeException("must be even number")
        val loop = length / 2
        items.clear()
        for (sequence in 1..loop) {
            items.add(Item(sequence, "", textColor = textColor))
            items.add(Item(sequence + loop, "", textColor = textColor))
        }
        notifyDataSetChanged()
    }

    fun set(words: Array<String>) {
        set(words, null)
    }

    fun set(words: Array<String>, hiddenSequences: IntArray? = null) {
        if (words.size % 2 != 0) throw RuntimeException("must be even number")
        items.clear()
        val loop = words.size / 2
        for (index in 0 until loop) {
            val sequence1 = index + 1
            val sequence2 = sequence1 + loop
            val word1 = words[sequence1 - 1]
            val word2 = words[sequence2 - 1]

            items.add(
                Item(
                    sequence1,
                    word1,
                    hiddenSequences?.contains(sequence1) ?: false,
                    textColor = textColor
                )
            )
            items.add(
                Item(
                    sequence2,
                    word2,
                    hiddenSequences?.contains(sequence2) ?: false,
                    textColor = textColor
                )
            )
        }
        notifyDataSetChanged()
    }

    fun getHiddenWords() = items.filter { i -> i.hidden }.map { i -> i.word }

    fun set(word: String) {
        val focusedIndex = items.indexOfFirst { i -> i.focused }
        if (focusedIndex == -1) return
        items[focusedIndex].word = word
        notifyItemChanged(focusedIndex)
    }

    fun show(word: String) {
        val item = items.find { i -> i.hidden && i.focused } ?: return
        item.hidden = false
        item.word = word
        item.focused = false
        item.textColor = R.color.blue_ribbon
        notifyItemChanged(items.indexOf(item))
        requestNextHiddenFocus()
    }

    fun hide(sequence: Int) {
        val item = items.firstOrNull { i -> i.sequence == sequence } ?: return
        if (item.hidden) return
        clearFocus()
        item.hidden = true
        item.focused = true
        notifyItemChanged(items.indexOf(item))
    }

    fun countHidden() = items.count { i -> i.hidden }

    fun isItemsVisible() = this.items.find { i -> i.hidden } == null

    fun compare(words: Array<String>): Boolean {
        if (words.size != items.size) return false
        val sortedWords =
            this.items.sortedBy { i -> i.sequence }.map { i -> i.word }
                .toTypedArray()
        return words contentDeepEquals sortedWords
    }

    fun setTextColor(@ColorRes color: Int, indexes: IntArray? = null) {
        if (indexes == null) {
            this.items.forEach { i -> i.textColor = color }
        } else {
            for (item in items) {
                if (indexes.contains(item.sequence)) {
                    item.textColor = color
                } else {
                    item.textColor = textColor
                }
            }
        }

        notifyDataSetChanged()
    }

    fun requestNextHiddenFocus() {
        val hiddenItems = items.filter { i -> i.hidden }.toMutableList()
        if (hiddenItems.isEmpty()) return
        hiddenItems.sortWith(Comparator { o1, o2 -> o1.sequence.compareTo(o2.sequence) })
        clearFocus()
        val firstHiddenItem = hiddenItems.first()
        firstHiddenItem.focused = true
        notifyItemChanged(this.items.indexOf(firstHiddenItem))
    }

    fun getPhrase(): Array<String?> {
        val phrase = arrayOfNulls<String>(items.size)
        for (item in items) {
            phrase[item.sequence - 1] = item.word
        }
        return phrase
    }

    fun isValid() = null == items.find { it.word.isEmpty() }

    fun requestFocus(sequence: Int) {
        clearFocus()
        val item = items.find { i -> i.sequence == sequence } ?: return
        item.focused = true
        notifyItemChanged(items.indexOf(item))
    }

    override fun afterTextChanged(item: Item) {
        items.find { it.sequence == item.sequence }
            ?.also { it.word = item.word }
        onTextChangeListener?.afterTextChanged(item)
    }

    override fun onTextChanged(item: Item) {
        onTextChangeListener?.onTextChanged(item)
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_recovery_phrase, parent, false)
        return ViewHolder(view, editable, this, itemClickListener)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }

    inner class ViewHolder(
        view: View,
        editable: Boolean,
        private val listener: OnTextChangeListener,
        itemClickListener: (Int) -> Unit
    ) :
        RecyclerView.ViewHolder(view) {

        private lateinit var item: Item

        init {
            with(itemView) {
                if (!editable) {
                    edtWord.isFocusable = false
                    edtWord.isLongClickable = false
                    edtWord.isCursorVisible = false
                }

                edtWord.doOnTextChanged { text, _, _, _ ->
                    item.word = text.toString()

                    edtWord.background = ContextCompat.getDrawable(
                        context,
                        if (text.isNullOrBlank()) {
                            R.drawable.bg_border_blue_ribbon_wild_sand
                        } else {
                            R.drawable.bg_border_blue_ribbon_white
                        }
                    )

                    listener.onTextChanged(item)
                }

                edtWord.doAfterTextChanged {
                    listener.afterTextChanged(item)
                }

                edtWord.setOnFocusChangeListener { _, hasFocus ->
                    item.focused = hasFocus
                    val text = edtWord.text

                    edtWord.imeOptions =
                        if (hasFocus && (isLastItem() || isValid())) EditorInfo.IME_ACTION_DONE else EditorInfo.IME_ACTION_NEXT

                    if (!hasFocus) {
                        edtWord.background = if (text.isNullOrBlank()) {
                            ContextCompat.getDrawable(
                                context,
                                R.color.wild_sand
                            )
                        } else {
                            null
                        }
                    } else {
                        edtWord.setSelection(edtWord.text?.length ?: 0)
                        edtWord.background = ContextCompat.getDrawable(
                            context,
                            if (text.isNullOrBlank()) {
                                R.drawable.bg_border_blue_ribbon_wild_sand
                            } else {
                                R.drawable.bg_border_blue_ribbon_white
                            }
                        )
                    }
                }

                edtWord.setOnEditorActionListener { _, actionId, event ->
                    if (event == null) {
                        when (actionId) {
                            EditorInfo.IME_ACTION_NEXT -> {
                                requestNextFocus()
                                true
                            }

                            EditorInfo.IME_ACTION_DONE -> {
                                edtWord.clearFocus()
                                onDoneListener?.invoke()
                                true
                            }

                            else -> false
                        }
                    } else false
                }

                edtWord.setOnClickListener {
                    itemClickListener(adapterPosition)
                }
            }
        }

        fun bind(item: Item) {
            this.item = item
            with(itemView) {
                tvNo.text = "%d.".format(item.sequence)
                edtWord.setText(if (item.hidden) "" else item.word)
                edtWord.setTextColor(
                    ContextCompat.getColor(
                        context,
                        item.textColor
                    )
                )

                if (item.focused) {
                    edtWord.requestFocus()
                    edtWord.background = ContextCompat.getDrawable(
                        context,
                        R.drawable.bg_border_blue_ribbon_wild_sand
                    )
                } else {
                    edtWord.clearFocus()
                    edtWord.background =
                        ContextCompat.getDrawable(
                            context,
                            if (item.hidden || item.word.isBlank()) R.color.wild_sand else android.R.color.white
                        )
                }

            }

        }

        fun isLastItem() = item.sequence == items.size
    }
}

interface OnTextChangeListener {

    fun onTextChanged(item: Item)

    fun afterTextChanged(item: Item)
}

class Item(
    internal val sequence: Int,
    internal var word: String,
    internal var hidden: Boolean = false,
    internal var textColor: Int = R.color.gray,
    internal var focused: Boolean = false
) {
    fun isHidden() = hidden
}

enum class Version(val value: Int) {
    TWELVE(12),
    TWENTY_FOUR(24)
}