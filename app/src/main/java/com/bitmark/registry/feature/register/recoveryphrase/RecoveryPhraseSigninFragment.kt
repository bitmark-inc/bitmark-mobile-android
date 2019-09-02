package com.bitmark.registry.feature.register.recoveryphrase

import android.os.Bundle
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SimpleItemAnimator
import com.bitmark.registry.R
import com.bitmark.registry.feature.BaseSupportFragment
import com.bitmark.registry.feature.BaseViewModel
import com.bitmark.registry.feature.Navigator
import com.bitmark.registry.feature.register.authentication.AuthenticationFragment
import com.bitmark.registry.util.extension.*
import com.bitmark.registry.util.view.SimpleRecyclerViewAdapter
import com.bitmark.sdk.features.Account
import com.bitmark.sdk.features.internal.RecoveryPhrase
import kotlinx.android.synthetic.main.fragment_recovery_phrase_signin.*
import java.util.*
import javax.inject.Inject


/**
 * @author Hieu Pham
 * @since 7/5/19
 * Email: hieupham@bitmark.com
 * Copyright © 2019 Bitmark. All rights reserved.
 */
class RecoveryPhraseSigninFragment : BaseSupportFragment() {

    companion object {
        private const val URI = "uri"

        fun newInstance(uri: String? = null): RecoveryPhraseSigninFragment {
            val bundle = Bundle()
            if (uri != null) bundle.putString(URI, uri)
            val fragment = RecoveryPhraseSigninFragment()
            fragment.arguments = bundle
            return fragment
        }
    }

    @Inject
    internal lateinit var navigator: Navigator

    override fun layoutRes(): Int = R.layout.fragment_recovery_phrase_signin

    override fun viewModel(): BaseViewModel? = null

    override fun initComponents() {
        super.initComponents()

        var locale = Locale.getDefault()
        if (locale != Locale.ENGLISH && locale != Locale.TRADITIONAL_CHINESE) {
            locale = Locale.ENGLISH
        }
        val bip39Words = RecoveryPhrase.getWords(locale)

        val phraseAdapter =
            RecoveryPhraseAdapter(textColor = R.color.blue_ribbon)
        val phraseLayoutManager =
            GridLayoutManager(context, 2, RecyclerView.VERTICAL, false)
        rvRecoveryPhrase.layoutManager = phraseLayoutManager
        rvRecoveryPhrase.isNestedScrollingEnabled = false
        (rvRecoveryPhrase.itemAnimator as? SimpleItemAnimator)?.supportsChangeAnimations =
            false
        rvRecoveryPhrase.adapter = phraseAdapter
        phraseAdapter.setDefault()

        val suggestionAdapter =
            SimpleRecyclerViewAdapter(R.layout.item_text_suggestion)
        val suggestionLayoutManager =
            LinearLayoutManager(context, RecyclerView.HORIZONTAL, false)
        rvSuggestion.layoutManager = suggestionLayoutManager
        rvSuggestion.isNestedScrollingEnabled = false
        (rvSuggestion.itemAnimator as? SimpleItemAnimator)?.supportsChangeAnimations =
            false
        rvSuggestion.adapter = suggestionAdapter

        activity?.detectKeyBoardState { showing ->
            if (view == null) return@detectKeyBoardState
            if (showing) {
                tvSwitchWord.gone()
                btnSubmit.gone()
                setSuggestionVisibility(true)
            } else {
                tvSwitchWord.visible()
                btnSubmit.visible()
                setSuggestionVisibility(false)
                if (phraseAdapter.isValid()) {
                    phraseAdapter.clearFocus()
                }
            }
        }

        btnSubmit.setSafetyOnclickListener {
            submit(phraseAdapter.getPhrase())
        }

        tvSwitchWord.setOnClickListener {
            setErrorVisibility(false)
            if (phraseAdapter.itemCount == Version.TWELVE.value) {
                tvInstruction.setText(R.string.please_type_all_24_word)
                tvSwitchWord.setText(R.string.are_you_using_12_word)
                phraseAdapter.setDefault(Version.TWENTY_FOUR)
            } else {
                tvInstruction.setText(R.string.please_type_all_12_word)
                tvSwitchWord.setText(R.string.are_you_using_24_word)
                phraseAdapter.setDefault()
            }
        }

        ivBack.setSafetyOnclickListener {
            navigator.popFragment()
        }

        phraseAdapter.setOnTextChangeListener(object : OnTextChangeListener {
            override fun onTextChanged(item: Item) {
                if (phraseAdapter.isValid()) {
                    btnSubmit.enable()
                } else {
                    btnSubmit.disable()
                }

                if (item.word.isBlank()) {
                    suggestionAdapter.clear()
                } else {
                    val matchedItems = bip39Words.filter { w ->
                        w.startsWith(
                            item.word,
                            ignoreCase = true
                        )
                    }
                    if (matchedItems.isEmpty()) return
                    suggestionAdapter.set(matchedItems, 3)
                }
            }

            override fun afterTextChanged(item: Item) {
            }

        })

        phraseAdapter.setOnDoneListener {
            submit(phraseAdapter.getPhrase())
        }

        suggestionAdapter.setItemClickListener { text ->
            phraseAdapter.set(text)
            if (!phraseAdapter.requestNextFocus()) {
                activity?.hideKeyBoard()
            }
        }

        ivUp.setOnClickListener {
            if (!phraseAdapter.requestPrevFocus()) {
                activity?.hideKeyBoard()
            }
        }

        ivDown.setOnClickListener {
            if (!phraseAdapter.requestNextFocus()) {
                activity?.hideKeyBoard()
            }
        }
    }

    private fun setSuggestionVisibility(visible: Boolean) {
        if (visible) {
            ivUp.visible()
            ivDown.visible()
            rvSuggestion.visible()
        } else {
            ivUp.gone()
            ivDown.gone()
            rvSuggestion.gone()
        }
    }

    private fun submit(phrase: Array<String?>) {
        activity?.hideKeyBoard()
        val isValid = try {
            Account.fromRecoveryPhrase(*phrase)
            true
        } catch (e: Throwable) {
            false
        }

        if (isValid) {
            setErrorVisibility(false)
            navigator.anim(Navigator.RIGHT_LEFT).replaceFragment(
                R.id.layoutContainer,
                AuthenticationFragment.newInstance(
                    phrase, arguments?.getString(
                        URI
                    )
                )
            )
        } else {
            setErrorVisibility(true)
        }
    }

    private fun setErrorVisibility(visible: Boolean) {
        if (visible) {
            tvError.visible()
            tvTryAgain.visible()
        } else {
            tvError.gone()
            tvTryAgain.gone()
        }
    }

    override fun onBackPressed() = navigator.popFragment() ?: false
}