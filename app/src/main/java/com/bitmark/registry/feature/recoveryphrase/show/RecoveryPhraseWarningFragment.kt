package com.bitmark.registry.feature.recoveryphrase.show

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.lifecycle.Observer
import com.bitmark.registry.R
import com.bitmark.registry.feature.BaseSupportFragment
import com.bitmark.registry.feature.BaseViewModel
import com.bitmark.registry.feature.DialogController
import com.bitmark.registry.feature.Navigator
import com.bitmark.registry.feature.Navigator.Companion.RIGHT_LEFT
import com.bitmark.registry.util.extension.loadAccount
import com.bitmark.registry.util.extension.setSafetyOnclickListener
import com.bitmark.sdk.authentication.KeyAuthenticationSpec
import com.bitmark.sdk.features.Account
import kotlinx.android.synthetic.main.fragment_recovery_warning.*
import java.util.*
import javax.inject.Inject


/**
 * @author Hieu Pham
 * @since 2019-07-24
 * Email: hieupham@bitmark.com
 * Copyright © 2019 Bitmark. All rights reserved.
 */
class RecoveryPhraseWarningFragment : BaseSupportFragment() {

    companion object {

        private const val TOOLBAR_TITLE = "toolbar_title"
        private const val WARNING_MESSAGE = "warning_message"

        fun newInstance(
            toolBarTitle: String,
            warningMessage: String
        ): RecoveryPhraseWarningFragment {
            val bundle = Bundle()
            bundle.putString(TOOLBAR_TITLE, toolBarTitle)
            bundle.putString(WARNING_MESSAGE, warningMessage)
            val fragment = RecoveryPhraseWarningFragment()
            fragment.arguments = bundle
            return fragment
        }
    }

    @Inject
    lateinit var viewModel: RecoveryPhraseWarningViewModel

    @Inject
    lateinit var navigator: Navigator

    @Inject
    lateinit var dialogController: DialogController

    override fun layoutRes(): Int = R.layout.fragment_recovery_warning

    override fun viewModel(): BaseViewModel? = null

    override fun initComponents() {
        super.initComponents()

        val toolBarTitle = arguments?.getString(TOOLBAR_TITLE) ?: ""
        val warningMessage = arguments?.getString(WARNING_MESSAGE) ?: ""

        tvToolbarTitle.text = toolBarTitle
        tvWarningContent.text = warningMessage

        ivBack.setOnClickListener { navigator.popChildFragment() }

        btnWriteDownRecoveryPhrase.setSafetyOnclickListener { viewModel.getAccountNumber() }

    }

    override fun observe() {
        super.observe()

        viewModel.getAccountNumberLiveData().observe(this, Observer { res ->
            when {
                res.isSuccess() -> {
                    val accountNumber = res.data()!!

                    loadAccount(accountNumber) { account ->

                        var locale = Locale.getDefault()
                        if (locale != Locale.ENGLISH && locale != Locale.TRADITIONAL_CHINESE) {
                            locale = Locale.ENGLISH
                        }

                        val recoveryPhrase =
                            account.getRecoveryPhrase(locale)
                                .mnemonicWords
                        navigator.anim(RIGHT_LEFT).replaceChildFragment(
                            R.id.layoutContainer,
                            RecoveryPhraseShowingFragment.newInstance(
                                recoveryPhrase
                            )
                        )

                    }
                }
            }
        })
    }

    private fun loadAccount(accountNumber: String, action: (Account) -> Unit) {
        val spec = KeyAuthenticationSpec.Builder(context)
            .setAuthenticationDescription(getString(R.string.please_sign_to_access_recovery_phrase))
            .setKeyAlias(accountNumber).build()
        activity?.loadAccount(
            accountNumber,
            spec,
            dialogController,
            successAction = action,
            setupRequiredAction = { gotoSecuritySetting() }
        )
    }

    private fun gotoSecuritySetting() {
        val intent = Intent(Settings.ACTION_SECURITY_SETTINGS)
        navigator.anim(Navigator.BOTTOM_UP).startActivity(intent)
    }

    override fun onBackPressed() = navigator.popChildFragment() ?: false
}