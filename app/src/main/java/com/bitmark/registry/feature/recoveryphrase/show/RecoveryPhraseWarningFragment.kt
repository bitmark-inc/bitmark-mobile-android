package com.bitmark.registry.feature.recoveryphrase.show

import android.os.Bundle
import androidx.lifecycle.Observer
import com.bitmark.registry.R
import com.bitmark.registry.logging.Tracer
import com.bitmark.registry.feature.BaseSupportFragment
import com.bitmark.registry.feature.BaseViewModel
import com.bitmark.registry.feature.DialogController
import com.bitmark.registry.feature.Navigator
import com.bitmark.registry.feature.Navigator.Companion.RIGHT_LEFT
import com.bitmark.registry.logging.Event
import com.bitmark.registry.logging.EventLogger
import com.bitmark.registry.feature.register.RegisterContainerActivity
import com.bitmark.registry.util.extension.gotoSecuritySetting
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

        private const val REMOVE_ACCESS = "remove_access"

        private const val TAG = "RecoveryPhraseWarningFragment"

        fun newInstance(
            toolBarTitle: String,
            warningMessage: String,
            removeAccess: Boolean = false
        ): RecoveryPhraseWarningFragment {
            val bundle = Bundle()
            bundle.putString(TOOLBAR_TITLE, toolBarTitle)
            bundle.putString(WARNING_MESSAGE, warningMessage)
            bundle.putBoolean(REMOVE_ACCESS, removeAccess)
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

    @Inject
    lateinit var logger: EventLogger

    private var removeAccess = false

    override fun layoutRes(): Int = R.layout.fragment_recovery_warning

    override fun viewModel(): BaseViewModel? = null

    override fun initComponents() {
        super.initComponents()

        val toolBarTitle = arguments?.getString(TOOLBAR_TITLE) ?: ""
        val warningMessage = arguments?.getString(WARNING_MESSAGE) ?: ""
        removeAccess = arguments?.getBoolean(REMOVE_ACCESS) ?: false

        tvToolbarTitle.text = toolBarTitle
        tvWarningContent.text = warningMessage

        ivBack.setOnClickListener { navigator.popChildFragment() }

        btnWriteDownRecoveryPhrase.setSafetyOnclickListener { viewModel.getAccountInfo() }

    }

    override fun deinitComponents() {
        dialogController.dismiss()
        super.deinitComponents()
    }

    override fun observe() {
        super.observe()

        viewModel.getAccountInfoLiveData().observe(this, Observer { res ->
            when {
                res.isSuccess() -> {
                    val accountInfo = res.data()!!
                    val accountNumber = accountInfo.first
                    val keyAlias = accountInfo.second

                    loadAccount(accountNumber, keyAlias) { account ->

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
                                recoveryPhrase, removeAccess
                            )
                        )

                    }
                }

                res.isError() -> {
                    Tracer.ERROR.log(
                        TAG,
                        "get account info failed: ${res.throwable()
                            ?: "unknown"}"
                    )
                    dialogController.alert(
                        R.string.error,
                        R.string.unexpected_error,
                        R.string.ok
                    ) { navigator.popChildFragmentToRoot() }
                }
            }
        })
    }

    private fun loadAccount(
        accountNumber: String,
        keyAlias: String,
        action: (Account) -> Unit
    ) {
        val spec = KeyAuthenticationSpec.Builder(context)
            .setAuthenticationDescription(getString(R.string.your_authorization_is_required))
            .setKeyAlias(keyAlias).build()
        activity?.loadAccount(
            accountNumber,
            spec,
            dialogController,
            successAction = action,
            setupRequiredAction = { navigator.gotoSecuritySetting() },
            invalidErrorAction = { e ->
                Tracer.ERROR.log(
                    TAG,
                    "biometric authentication is invalidated: ${e?.message}"
                )
                logger.logError(Event.AUTH_INVALID_ERROR, e)
                dialogController.alert(
                    R.string.account_is_not_accessible,
                    R.string.sorry_you_have_changed_or_removed
                ) {
                    navigator.startActivityAsRoot(
                        RegisterContainerActivity::class.java,
                        RegisterContainerActivity.getBundle(recoverAccount = true)
                    )
                }
            }
        )
    }

    override fun onBackPressed() = navigator.popChildFragment() ?: false
}