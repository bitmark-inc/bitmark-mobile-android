package com.bitmark.registry.feature.register.recoveryphrase

import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bitmark.registry.R
import com.bitmark.registry.feature.BaseSupportFragment
import com.bitmark.registry.feature.BaseViewModel
import com.bitmark.registry.feature.Navigator
import com.bitmark.registry.feature.Navigator.Companion.RIGHT_LEFT
import com.bitmark.registry.feature.register.authentication.AuthenticationFragment
import com.bitmark.registry.util.extension.*
import com.bitmark.sdk.features.Account
import kotlinx.android.synthetic.main.fragment_recovery_phrase_signin.*
import javax.inject.Inject


/**
 * @author Hieu Pham
 * @since 7/5/19
 * Email: hieupham@bitmark.com
 * Copyright © 2019 Bitmark. All rights reserved.
 */
class RecoveryPhraseSigninFragment : BaseSupportFragment() {

    companion object {
        fun newInstance() = RecoveryPhraseSigninFragment()
    }

    @Inject
    internal lateinit var navigator: Navigator

    override fun layoutRes(): Int = R.layout.fragment_recovery_phrase_signin

    override fun viewModel(): BaseViewModel? = null

    override fun initComponents() {
        super.initComponents()

        val adapter = RecoveryPhraseAdapter(textColor = R.color.blue_ribbon)
        val layoutManager =
            GridLayoutManager(context, 2, RecyclerView.VERTICAL, false)
        rvRecoveryPhrase.layoutManager = layoutManager
        rvRecoveryPhrase.isNestedScrollingEnabled = false
        rvRecoveryPhrase.adapter = adapter
        adapter.setDefault()

        activity?.detectKeyBoardState { showing ->
            if (view == null) return@detectKeyBoardState
            if (showing) {
                tvSwitchWord.gone()
            } else {
                tvSwitchWord.visible()
            }
        }

        btnSubmit.setSafetyOnclickListener {
            activity?.hideKeyBoard()
            val phrase = adapter.getPhrase()
            val isValid = try {
                Account.fromRecoveryPhrase(*phrase)
                true
            } catch (e: Throwable) {
                false
            }

            if (isValid) {
                tvError.gone()
                tvTryAgain.gone()
                navigator.anim(RIGHT_LEFT).replaceFragment(
                    R.id.layoutContainer,
                    AuthenticationFragment.newInstance(phrase)
                )
            } else {
                tvError.visible()
                tvTryAgain.visible()
            }

        }

        tvSwitchWord.setOnClickListener {
            if (adapter.itemCount == Version.TWELVE.value) {
                tvInstruction.setText(R.string.please_type_all_24_word)
                tvSwitchWord.setText(R.string.are_you_using_12_word)
                adapter.setDefault(Version.TWENTY_FOUR)
            } else {
                tvInstruction.setText(R.string.please_type_all_12_word)
                tvSwitchWord.setText(R.string.are_you_using_24_word)
                adapter.setDefault()
            }
        }

        tvBack.setSafetyOnclickListener {
            navigator.popFragment()
        }

        adapter.setListener(object : OnTextChangeListener {
            override fun onTextChanged(item: Item) {
                if (adapter.isValid()) btnSubmit.enable()
                else btnSubmit.disable()
            }

            override fun afterTextChanged(item: Item) {
            }

        })
    }

    override fun onBackPressed() = navigator.popFragment() ?: false
}