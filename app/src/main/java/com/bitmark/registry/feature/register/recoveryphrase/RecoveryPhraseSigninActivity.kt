package com.bitmark.registry.feature.register.recoveryphrase

import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bitmark.registry.R
import com.bitmark.registry.feature.BaseAppCompatActivity
import com.bitmark.registry.feature.BaseViewModel
import com.bitmark.registry.feature.Navigator
import com.bitmark.registry.feature.Navigator.Companion.RIGHT_LEFT
import com.bitmark.registry.feature.register.authentication.AuthenticationActivity
import com.bitmark.registry.util.extension.*
import com.bitmark.sdk.features.Account
import kotlinx.android.synthetic.main.activity_recovery_phrase_signin.*
import javax.inject.Inject


/**
 * @author Hieu Pham
 * @since 7/5/19
 * Email: hieupham@bitmark.com
 * Copyright © 2019 Bitmark. All rights reserved.
 */
class RecoveryPhraseSigninActivity : BaseAppCompatActivity() {

    @Inject
    internal lateinit var navigator: Navigator<RecoveryPhraseSigninActivity>

    override fun layoutRes(): Int = R.layout.activity_recovery_phrase_signin

    override fun viewModel(): BaseViewModel? = null

    override fun initComponents() {
        super.initComponents()

        val adapter = RecoveryPhraseAdapter(textColor = R.color.blue_ribbon)
        val layoutManager =
            GridLayoutManager(this, 2, RecyclerView.VERTICAL, false)
        rvRecoveryPhrase.layoutManager = layoutManager
        rvRecoveryPhrase.isNestedScrollingEnabled = false
        rvRecoveryPhrase.adapter = adapter
        adapter.setDefault()

        detectKeyBoardState { showing ->
            if (showing) {
                tvSwitchWord.gone()
            } else {
                tvSwitchWord.visible()
            }
        }

        btnSubmit.setSafetyOnclickListener {
            hideKeyBoard()
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
                navigator.anim(Navigator.RIGHT_LEFT).startActivityAsRoot(
                    AuthenticationActivity::class.java,
                    AuthenticationActivity.getBundle(phrase)
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
            navigator.anim(RIGHT_LEFT).finishActivity()
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

    override fun onBackPressed() {
        navigator.anim(RIGHT_LEFT).finishActivity()
        super.onBackPressed()
    }
}