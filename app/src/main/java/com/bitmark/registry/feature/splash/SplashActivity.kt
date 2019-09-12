package com.bitmark.registry.feature.splash

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.text.TextUtils
import androidx.lifecycle.Observer
import com.bitmark.registry.R
import com.bitmark.registry.logging.Tracer
import com.bitmark.registry.feature.BaseAppCompatActivity
import com.bitmark.registry.feature.BaseViewModel
import com.bitmark.registry.feature.DialogController
import com.bitmark.registry.feature.Navigator
import com.bitmark.registry.feature.Navigator.Companion.RIGHT_LEFT
import com.bitmark.registry.logging.Event
import com.bitmark.registry.logging.EventLogger
import com.bitmark.registry.feature.main.MainActivity
import com.bitmark.registry.feature.notification.DeleteFirebaseInstanceIdService
import com.bitmark.registry.feature.register.RegisterContainerActivity
import com.bitmark.registry.util.extension.gone
import com.bitmark.registry.util.extension.gotoSecuritySetting
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

    companion object {

        private const val TAG = "SplashActivity"
    }

    @Inject
    internal lateinit var viewModel: SplashViewModel

    @Inject
    internal lateinit var dialogController: DialogController

    @Inject
    internal lateinit var navigator: Navigator

    @Inject
    internal lateinit var logger: EventLogger

    private lateinit var authorizationDialog: AuthorizationRequiredDialog

    private val handler = Handler()

    private var notificationData: Bundle? = null

    override fun layoutRes(): Int = R.layout.activity_splash

    override fun viewModel(): BaseViewModel? = viewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val notificationBundle = intent?.getBundleExtra("notification")
        if (notificationBundle != null) {
            val bundle = Bundle()
            bundle.putBundle("notification", notificationBundle)
            notificationData = bundle
        }
    }

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
        handler.removeCallbacksAndMessages(null)
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
                    val info = res.data() ?: return@Observer
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
                            val keyPair = account.authKeyPair
                            viewModel.prepareData(
                                keyPair,
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
                    Tracer.ERROR.log(
                        TAG,
                        "get existing account failed: ${res.throwable()
                            ?: "unknown"}"
                    )
                    dialogController.alert(
                        R.string.error,
                        R.string.unexpected_error
                    )
                }
            }
        })

        viewModel.prepareDataLiveData().observe(this, Observer { res ->
            when {
                res.isLoading() -> {
                    tvAction.setText(R.string.loading_three_dot)
                    showLoading()
                }

                res.isSuccess() -> {
                    handler.postDelayed({
                        hideLoading()
                        navigator.anim(RIGHT_LEFT)
                            .startActivityAsRoot(
                                MainActivity::class.java, notificationData
                            )
                    }, 500)
                }

                res.isError() -> {
                    Tracer.ERROR.log(
                        TAG,
                        "prepare data failed: ${res.throwable() ?: "unknown"}"
                    )
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
                        val intent = Intent(
                            this,
                            DeleteFirebaseInstanceIdService::class.java
                        )
                        startService(intent)
                    }
                    handler.postDelayed({
                        // a bit delay to avoid flash screen if nothing need to cleanup
                        hideLoading()
                        viewModel.getExistingAccount()
                    }, 500)
                }

                res.isError() -> {
                    Tracer.ERROR.log(
                        TAG,
                        "clean up app data failed: ${res.throwable()
                            ?: "unknown"}"
                    )
                    // TODO show alert and navigate to recovery phrase signin
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
                .setAuthenticationDescription(getString(R.string.your_authorization_is_required))
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
            })
    }

    private fun getFirebaseToken(action: (String?) -> Unit) {
        FirebaseInstanceId.getInstance()
            .instanceId.addOnCompleteListener { task ->
            if (!task.isSuccessful) action.invoke(null)
            else action.invoke(task.result?.token)
        }
    }
}