package com.bitmark.registry.feature.splash

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.provider.Settings
import android.text.TextUtils
import androidx.lifecycle.Observer
import com.bitmark.cryptography.crypto.Ed25519
import com.bitmark.cryptography.crypto.encoder.Hex.HEX
import com.bitmark.cryptography.crypto.encoder.Raw.RAW
import com.bitmark.registry.R
import com.bitmark.registry.feature.BaseAppCompatActivity
import com.bitmark.registry.feature.BaseViewModel
import com.bitmark.registry.feature.DialogController
import com.bitmark.registry.feature.Navigator
import com.bitmark.registry.feature.Navigator.Companion.BOTTOM_UP
import com.bitmark.registry.feature.Navigator.Companion.RIGHT_LEFT
import com.bitmark.registry.feature.main.MainActivity
import com.bitmark.registry.feature.register.RegisterContainerActivity
import com.bitmark.registry.util.extension.gone
import com.bitmark.registry.util.extension.loadAccount
import com.bitmark.registry.util.extension.visible
import com.bitmark.registry.util.view.AuthorizationRequiredDialog
import com.bitmark.sdk.authentication.KeyAuthenticationSpec
import com.bitmark.sdk.features.Account
import kotlinx.android.synthetic.main.activity_splash.*
import javax.inject.Inject


/**
 * @author Hieu Pham
 * @since 7/1/19
 * Email: hieupham@bitmark.com
 * Copyright © 2019 Bitmark. All rights reserved.
 */
class SplashActivity : BaseAppCompatActivity() {

    @Inject
    internal lateinit var viewModel: SplashViewModel

    @Inject
    internal lateinit var dialogController: DialogController

    @Inject
    internal lateinit var navigator: Navigator<SplashActivity>

    private lateinit var authorizationDialog: AuthorizationRequiredDialog

    override fun layoutRes(): Int = R.layout.activity_splash

    override fun viewModel(): BaseViewModel? = viewModel

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        process()
    }

    override fun initComponents() {
        super.initComponents()
        authorizationDialog = AuthorizationRequiredDialog(this) {
            process()
        }
    }

    override fun deinitComponents() {
        dialogController.dismiss()
        super.deinitComponents()
    }

    private fun process() = viewModel.getExistingAccount()

    override fun observe() {
        super.observe()
        viewModel.getExistingAccountLiveData().observe(this, Observer { res ->
            when {
                res.isSuccess() -> {
                    val info = res.data()!!
                    val accountNumber = info.first
                    val authRequired = info.second
                    val existing = !TextUtils.isEmpty(accountNumber)
                    if (existing) {
                        getStoredAccount(
                            accountNumber,
                            authRequired
                        ) { account ->

                            // register JWT
                            val timestamp =
                                System.currentTimeMillis().toString()
                            val signature = HEX.encode(
                                Ed25519.sign(
                                    RAW.decode(timestamp),
                                    account.keyPair.privateKey().toBytes()
                                )
                            )
                            viewModel.prepareData(
                                timestamp,
                                signature,
                                accountNumber
                            )
                        }
                    } else {
                        Handler().postDelayed({
                            navigator.anim(RIGHT_LEFT)
                                .startActivityAsRoot(RegisterContainerActivity::class.java)
                        }, 1000)
                    }

                }
                res.isError() -> {
                    dialogController.alert(
                        getString(R.string.error),
                        res.throwable()?.message!!
                    )
                }
            }
        })

        viewModel.prepareDataLiveData().observe(this, Observer { res ->
            when {
                res.isLoading() -> progressBar.visible()
                res.isSuccess() -> {
                    progressBar.gone()
                    Handler().postDelayed({
                        navigator.anim(RIGHT_LEFT)
                            .startActivityAsRoot(MainActivity::class.java)
                    }, 500)
                }
                else -> {
                    progressBar.gone()
                    dialogController.alert(
                        getString(R.string.error),
                        res.throwable()?.message!!
                    ) { }
                }
            }
        })
    }

    private fun getStoredAccount(
        accountNumber: String,
        authenticateRequired: Boolean,
        action: (Account) -> Unit
    ) {
        val spec =
            KeyAuthenticationSpec.Builder(this).setKeyAlias(accountNumber)
                .setAuthenticationRequired(authenticateRequired).build()
        loadAccount(
            accountNumber,
            spec,
            dialogController,
            successAction = { account ->
                dialogController.dismiss(authorizationDialog)
                action.invoke(account)
            },
            canceledAction = {
                dialogController.show(authorizationDialog)
            },
            setupRequiredAction = { gotoSecuritySetting() },
            unknownErrorAction = { e -> exitWithAlert(e?.message!!) })
    }


    private fun gotoSecuritySetting() {
        val intent = Intent(Settings.ACTION_SECURITY_SETTINGS)
        navigator.anim(BOTTOM_UP).startActivityAsRoot(intent)
    }

    private fun exitWithAlert(message: String) {
        dialogController.alert(
            getString(R.string.error),
            message
        ) { navigator.finishActivity() }
    }
}