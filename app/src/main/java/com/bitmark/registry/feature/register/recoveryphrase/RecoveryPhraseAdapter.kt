package com.bitmark.registry.feature.register.recoveryphrase

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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

    private var listener: OnTextChangeListener? = null

    fun setListener(listener: OnTextChangeListener?) {
        this.listener = listener
    }

    fun setDefault(version: Version = Version.TWELVE) {
        val length = version.value
        if (length % 2 != 0) throw RuntimeException("must be even number")
        val loop = length / 2
        items.clear()
        for (sequence in 1..loop) {
            items.add(Item(sequence, ""))
            items.add(Item(sequence + loop, ""))
        }
        notifyDataSetChanged()
    }

    fun set(words: Array<String>) {
        if (words.size % 2 != 0) throw RuntimeException("must be even number")
        items.clear()
        val loop = words.size / 2
        for (sequence in 1..loop) {
            items.add(Item(sequence, ""))
            items.add(Item(sequence + loop, ""))
        }
        notifyDataSetChanged()
    }

    fun set(words: Array<Item>) {
        if (words.size % 2 != 0) throw RuntimeException("must be even number")
        items.clear()
        items.addAll(words)
        notifyDataSetChanged()
    }

    fun getPhrase(): Array<String?> {
        val phrase = arrayOfNulls<String>(items.size)
        for (item in items) {
            phrase[item.sequence - 1] = item.word
        }
        return phrase
    }

    fun isValid() = null == items.find { it.word.isEmpty() }

    override fun afterTextChanged(item: Item) {
        items.find { it.sequence == item.sequence }
            ?.also { it.word = item.word }
        listener?.afterTextChanged(item)
    }

    override fun onTextChanged(item: Item) {
        listener?.onTextChanged(item)
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_recovery_phrase, parent, false)
        return ViewHolder(view, editable, textColor, this)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }

    class ViewHolder(
        view: View,
        editable: Boolean,
        textColor: Int,
        private val listener: OnTextChangeListener
    ) :
        RecyclerView.ViewHolder(view) {

        private lateinit var item: Item

        init {
            with(itemView) {
                if (!editable) {
                    edtWord.isFocusable = false
                }

                edtWord.setTextColor(
                    ContextCompat.getColor(
                        context,
                        textColor
                    )
                )

                edtWord.doOnTextChanged { text, _, _, _ ->
                    item.word = text.toString()

                    edtWord.background = ContextCompat.getDrawable(
                        context,
                        if (text.isNullOrBlank()) R.drawable.bg_border_blue_ribbon_wild_sand_stateful else R.drawable.bg_border_blue_ribbon_white_stateful
                    )

                    listener.onTextChanged(item)
                }

                edtWord.doAfterTextChanged {
                    listener.afterTextChanged(item)
                }

                edtWord.setOnFocusChangeListener { _, hasFocus ->
                    val text = edtWord.text
                    if (!hasFocus && !text.isNullOrBlank()) {
                        edtWord.background = null
                    } else {
                        edtWord.background = ContextCompat.getDrawable(
                            context,
                            if (text.isNullOrBlank()) R.drawable.bg_border_blue_ribbon_wild_sand_stateful else R.drawable.bg_border_blue_ribbon_white_stateful
                        )
                    }
                }
            }
        }

        fun bind(item: Item) {
            this.item = item
            itemView.tvNo.text = "%d.".format(item.sequence)
            itemView.edtWord.setText(item.word)
        }
    }
}

interface OnTextChangeListener {

    fun onTextChanged(item: Item)

    fun afterTextChanged(item: Item)
}

class Item(internal val sequence: Int, internal var word: String)

enum class Version(val value: Int) {
    TWELVE(12), TWENTY_FOUR(24)
}