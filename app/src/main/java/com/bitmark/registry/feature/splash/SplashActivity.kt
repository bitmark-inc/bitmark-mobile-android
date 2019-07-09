package com.bitmark.registry.feature.splash

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.provider.Settings
import android.text.TextUtils
import androidx.lifecycle.Observer
import com.bitmark.apiservice.utils.callback.Callback1
import com.bitmark.cryptography.crypto.Ed25519
import com.bitmark.cryptography.crypto.encoder.Hex.HEX
import com.bitmark.cryptography.crypto.encoder.Raw.RAW
import com.bitmark.registry.R
import com.bitmark.registry.feature.BaseAppCompatActivity
import com.bitmark.registry.feature.BaseViewModel
import com.bitmark.registry.feature.DialogController
import com.bitmark.registry.feature.Navigator
import com.bitmark.registry.feature.Navigator.Companion.RIGHT_LEFT
import com.bitmark.registry.feature.main.MainActivity
import com.bitmark.registry.feature.register.RegisterActivity
import com.bitmark.registry.util.extension.gone
import com.bitmark.registry.util.extension.visible
import com.bitmark.sdk.authentication.KeyAuthenticationSpec
import com.bitmark.sdk.authentication.error.AuthenticationException
import com.bitmark.sdk.authentication.error.AuthenticationException.Type.CANCELLED
import com.bitmark.sdk.authentication.error.AuthenticationRequiredException
import com.bitmark.sdk.authentication.error.AuthenticationRequiredException.BIOMETRIC
import com.bitmark.sdk.authentication.error.AuthenticationRequiredException.FINGERPRINT
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
    internal lateinit var dialogController: DialogController<SplashActivity>

    @Inject
    internal lateinit var navigator: Navigator<SplashActivity>

    override fun layoutRes(): Int = R.layout.activity_splash

    override fun viewModel(): BaseViewModel? = viewModel

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        viewModel.getExistingAccount()
    }

    override fun onDestroy() {
        dialogController.dismiss()
        super.onDestroy()
    }

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
                            viewModel.registerJwt(
                                timestamp,
                                signature,
                                accountNumber
                            )
                        }
                    } else {
                        Handler().postDelayed({
                            navigator.anim(RIGHT_LEFT)
                                .startActivityAsRoot(RegisterActivity::class.java)
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

        viewModel.registerJwtLiveData().observe(this, Observer { res ->
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
        Account.loadFromKeyStore(
            this,
            accountNumber,
            spec,
            object : Callback1<Account> {
                override fun onSuccess(account: Account?) {
                    action.invoke(account!!)
                }

                override fun onError(throwable: Throwable?) {
                    when (throwable) {

                        // authentication error
                        is AuthenticationException -> {
                            when (throwable.type) {
                                // action cancel authentication
                                CANCELLED -> {
                                    dialogController.confirm(
                                        R.string.error,
                                        R.string.authentication_required,
                                        positiveEvent = {
                                            viewModel.getExistingAccount()
                                        },
                                        negativeEvent = {
                                            navigator.finishActivity()
                                        })
                                }

                                // other cases include error
                                else -> {
                                    exitWithAlert(throwable.message!!)
                                }
                            }
                        }

                        // missing security requirement
                        is AuthenticationRequiredException -> {
                            when (throwable.type) {

                                // did not set up fingerprint/biometric
                                FINGERPRINT, BIOMETRIC -> {
                                    dialogController.alert(
                                        R.string.error,
                                        R.string.fingerprint_required
                                    ) { gotoSecuritySetting() }
                                }

                                // did not set up pass code
                                else -> {
                                    dialogController.alert(
                                        R.string.error,
                                        R.string.passcode_pin_required
                                    ) { gotoSecuritySetting() }
                                }
                            }
                        }
                        else -> {
                            exitWithAlert(throwable?.message!!)
                        }
                    }
                }

            })
    }


    private fun gotoSecuritySetting() {
        val intent = Intent(Settings.ACTION_SECURITY_SETTINGS)
        navigator.startActivityAsRoot(intent)
    }

    private fun exitWithAlert(message: String) {
        dialogController.alert(
            getString(R.string.error),
            message
        ) { navigator.finishActivity() }
    }
}