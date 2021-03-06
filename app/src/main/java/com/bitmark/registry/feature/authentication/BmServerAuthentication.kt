/**
 * SPDX-License-Identifier: ISC
 * Copyright © 2014-2019 Bitmark. All rights reserved.
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 */
package com.bitmark.registry.feature.authentication

import android.app.Activity
import android.content.Context
import com.bitmark.cryptography.crypto.Ed25519
import com.bitmark.cryptography.crypto.encoder.Hex
import com.bitmark.cryptography.crypto.encoder.Raw
import com.bitmark.cryptography.crypto.key.KeyPair
import com.bitmark.registry.AppLifecycleHandler
import com.bitmark.registry.R
import com.bitmark.registry.data.source.AccountRepository
import com.bitmark.registry.feature.DialogController
import com.bitmark.registry.feature.Navigator
import com.bitmark.registry.feature.connectivity.ConnectivityHandler
import com.bitmark.registry.feature.register.RegisterContainerActivity
import com.bitmark.registry.feature.splash.SplashActivity
import com.bitmark.registry.logging.Event
import com.bitmark.registry.logging.EventLogger
import com.bitmark.registry.logging.Tracer
import com.bitmark.registry.util.extension.gotoSecuritySetting
import com.bitmark.registry.util.extension.loadAccount
import com.bitmark.registry.util.view.AuthorizationRequiredDialog
import com.bitmark.sdk.authentication.KeyAuthenticationSpec
import com.bitmark.sdk.features.Account
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.functions.BiFunction
import io.reactivex.schedulers.Schedulers

class BmServerAuthentication(
    private val context: Context,
    private val appLifecycleHandler: AppLifecycleHandler,
    private val connectivityHandler: ConnectivityHandler,
    private val accountRepo: AccountRepository,
    private val logger: EventLogger
) : AppLifecycleHandler.AppStateChangedListener,
    ConnectivityHandler.NetworkStateChangeListener {

    companion object {
        private const val TAG = "BmServerAuthentication"
    }

    private val compositeDisposable = CompositeDisposable()

    private var authorizationRequiredDialog: AuthorizationRequiredDialog? = null

    private var isProcessing = false

    init {
        appLifecycleHandler.addAppStateChangedListener(this)
        connectivityHandler.addNetworkStateChangeListener(this)
    }

    fun destroy() {
        compositeDisposable.dispose()
        connectivityHandler.removeNetworkStateChangeListener(this)
        appLifecycleHandler.removeAppStateChangedListener(this)
    }

    override fun onForeground() {
        super.onForeground()
        checkJwtExpiry()
    }

    override fun onChange(connected: Boolean) {
        if (!connected) return
        checkJwtExpiry()
    }

    private fun checkJwtExpiry() {
        if (isProcessing) return
        compositeDisposable.add(accountRepo.checkMobileServerJwtExpiry().flatMap { expired ->
            if (expired) {
                Single.zip(
                    accountRepo.getAccountNumber(),
                    accountRepo.getKeyAlias(),
                    BiFunction<String, String, Pair<String, String>> { accountNumber, keyAlias ->
                        Pair(accountNumber, keyAlias)
                    })
            } else {
                Single.just(Pair("", ""))
            }
        }.doOnSubscribe {
            isProcessing = true
        }.observeOn(AndroidSchedulers.mainThread()).subscribe { p, e ->
            isProcessing = false
            val activity = appLifecycleHandler.getRunningActivity()
            if (e == null && activity != null && p.first.isNotEmpty() && p.second.isNotEmpty()) {
                loadAccount(activity, p.first, p.second) { account ->
                    refreshJwt(account.accountNumber, account.authKeyPair)
                }
            }
        })
    }

    private fun refreshJwt(requester: String, keyPair: KeyPair) {
        compositeDisposable.add(
            Single.create<Triple<String, String, String>> { emt ->
                val timestamp =
                    System.currentTimeMillis().toString()
                val signature = Hex.HEX.encode(
                    Ed25519.sign(
                        Raw.RAW.decode(timestamp),
                        keyPair.privateKey().toBytes()
                    )
                )
                emt.onSuccess(Triple(timestamp, signature, requester))
            }.subscribeOn(
                Schedulers.io()
            ).flatMap { t ->
                accountRepo.registerMobileServerJwt(t.first, t.second, t.third)
            }.retry(3).observeOn(AndroidSchedulers.mainThread())
                .subscribe { _, e ->
                    if (e != null) {
                        // critical fault, reload app
                        val activity =
                            appLifecycleHandler.getRunningActivity()
                                ?: return@subscribe
                        DialogController(activity).alert(
                            R.string.error,
                            R.string.unexpected_error
                        ) {
                            Navigator(activity).startActivityAsRoot(
                                SplashActivity::class.java
                            )
                        }
                    } else {
                        authorizationRequiredDialog?.dismiss()
                    }
                }
        )
    }

    private fun loadAccount(
        activity: Activity,
        accountNumber: String,
        keyAlias: String,
        action: (Account) -> Unit
    ) {
        val spec =
            KeyAuthenticationSpec.Builder(context)
                .setAuthenticationDescription(context.getString(R.string.your_authorization_is_required))
                .setKeyAlias(keyAlias).build()
        activity.loadAccount(accountNumber,
            spec,
            DialogController(activity),
            successAction = action,
            setupRequiredAction = { Navigator(activity).gotoSecuritySetting() },
            canceledAction = {
                if (authorizationRequiredDialog?.isShowing == true) return@loadAccount
                authorizationRequiredDialog =
                    AuthorizationRequiredDialog(activity) { checkJwtExpiry() }
                authorizationRequiredDialog?.show()
            }, invalidErrorAction = { e ->
                Tracer.ERROR.log(
                    TAG,
                    "biometric authentication is invalidated: ${e?.message}"
                )
                logger.logError(Event.AUTH_INVALID_ERROR, e)
                DialogController(activity).alert(
                    R.string.account_is_not_accessible,
                    R.string.sorry_you_have_changed_or_removed
                ) {
                    Navigator(activity).startActivityAsRoot(
                        RegisterContainerActivity::class.java,
                        RegisterContainerActivity.getBundle(recoverAccount = true)
                    )
                }
            })
    }
}