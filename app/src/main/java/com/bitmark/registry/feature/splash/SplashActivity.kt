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
import com.google.firebase.iid.FirebaseInstanceId
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
    internal lateinit var navigator: Navigator

    private lateinit var authorizationDialog: AuthorizationRequiredDialog

    private val handler = Handler()

    override fun layoutRes(): Int = R.layout.activity_splash

    override fun viewModel(): BaseViewModel? = viewModel

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        process()
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        super.onDestroy()
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

    private fun process() {
        getFirebaseToken { token -> viewModel.cleanupAppData(token) }
    }

    override fun observe() {
        super.observe()
        viewModel.getExistingAccountLiveData().observe(this, Observer { res ->
            when {
                res.isSuccess() -> {
                    val info = res.data()!!
                    val accountNumber = info.first
                    val authRequired = info.second
                    val keyAlias = info.third
                    val existing = !TextUtils.isEmpty(accountNumber)
                    if (existing) {
                        getStoredAccount(
                            accountNumber,
                            authRequired,
                            keyAlias
                        ) { account ->

                            // prepare data
                            val keyPair = account.keyPair
                            val timestamp =
                                System.currentTimeMillis().toString()
                            val signature = HEX.encode(
                                Ed25519.sign(
                                    RAW.decode(timestamp),
                                    keyPair.privateKey().toBytes()
                                )
                            )
                            viewModel.prepareData(
                                keyPair,
                                timestamp,
                                signature,
                                accountNumber
                            )
                        }
                    } else {
                        handler.postDelayed({
                            navigator.anim(RIGHT_LEFT)
                                .startActivityAsRoot(RegisterContainerActivity::class.java)
                        }, 250)
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
                res.isLoading() -> {
                    tvAction.setText(R.string.preparing_data)
                    showLoading()
                }

                res.isSuccess() -> {
                    handler.postDelayed({
                        hideLoading()
                        navigator.anim(RIGHT_LEFT)
                            .startActivityAsRoot(MainActivity::class.java)
                    }, 500)
                }

                res.isError() -> {
                    hideLoading()
                    dialogController.alert(
                        getString(R.string.error),
                        res.throwable()?.message!!
                    ) { }
                }
            }
        })

        viewModel.cleanupAppDataLiveData().observe(this, Observer { res ->
            when {
                res.isSuccess() -> {
                    val dataDeleted = res.data()!!
                    if (dataDeleted) {
                        FirebaseInstanceId.getInstance().deleteInstanceId()
                    }
                    handler.postDelayed({
                        // a bit delay to avoid flash screen if nothing need to cleanup
                        hideLoading()
                        viewModel.getExistingAccount()
                    }, 500)
                }

                res.isError() -> {
                    // TODO show alert and navigate to recovery phrase signin
                }

                res.isLoading() -> {
                    tvAction.setText(R.string.clean_up_data)
                    showLoading()
                }
            }
        })

        viewModel.progressLiveData.observe(this, Observer { progress ->
            progressBar.progress = progress
        })
    }

    private fun showLoading() {
        tvAction.visible()
        progressBar.visible()
    }

    private fun hideLoading() {
        tvAction.gone()
        progressBar.gone()
    }

    private fun getStoredAccount(
        accountNumber: String,
        authenticateRequired: Boolean,
        keyAlias: String,
        action: (Account) -> Unit
    ) {
        val spec =
            KeyAuthenticationSpec.Builder(this).setKeyAlias(keyAlias)
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

    private fun getFirebaseToken(action: (String?) -> Unit) {
        FirebaseInstanceId.getInstance()
            .instanceId.addOnCompleteListener { task ->
            if (!task.isSuccessful) action.invoke(null)
            else action.invoke(task.result?.token)
        }
    }
}