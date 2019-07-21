package com.bitmark.registry.feature.register.authentication

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.provider.Settings
import androidx.lifecycle.Observer
import com.bitmark.apiservice.utils.callback.Callback0
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
import com.bitmark.registry.util.extension.gone
import com.bitmark.registry.util.extension.setSafetyOnclickListener
import com.bitmark.registry.util.extension.visible
import com.bitmark.sdk.authentication.KeyAuthenticationSpec
import com.bitmark.sdk.authentication.error.AuthenticationException
import com.bitmark.sdk.authentication.error.AuthenticationRequiredException
import com.bitmark.sdk.features.Account
import kotlinx.android.synthetic.main.activity_authentication.*
import javax.inject.Inject


/**
 * @author Hieu Pham
 * @since 7/5/19
 * Email: hieupham@bitmark.com
 * Copyright © 2019 Bitmark. All rights reserved.
 */
class AuthenticationActivity : BaseAppCompatActivity() {

    companion object {
        private const val RECOVERY_PHRASE = "recovery_phrase"

        fun getBundle(phrase: Array<String?>? = null): Bundle {
            val bundle = Bundle()
            if (null != phrase) bundle.putStringArray(RECOVERY_PHRASE, phrase)
            return bundle
        }
    }

    @Inject
    internal lateinit var viewModel: AuthenticationViewModel

    @Inject
    internal lateinit var dialogController: DialogController

    @Inject
    internal lateinit var navigator: Navigator<AuthenticationActivity>

    override fun layoutRes(): Int = R.layout.activity_authentication

    override fun viewModel(): BaseViewModel? = viewModel

    override fun onDestroy() {
        dialogController.dismiss()
        super.onDestroy()
    }

    override fun initComponents() {
        super.initComponents()

        val phrase = intent.extras?.getStringArray(RECOVERY_PHRASE)

        btnEnableTouchId.setSafetyOnclickListener {
            createAccount(
                phrase,
                true
            )
        }

        btnSkip.setSafetyOnclickListener {
            dialogController.confirm(
                title = R.string.warning,
                message = R.string.are_you_sure_you_dont_to_protect,
                positive = android.R.string.yes,
                positiveEvent = { createAccount(phrase, false) },
                negative = android.R.string.no
            )
        }
    }

    override fun observe() {
        super.observe()
        viewModel.registerAccountLiveData().observe(this, Observer { res ->
            when {
                res.isSuccess() -> {
                    Handler().postDelayed({
                        navigator.anim(RIGHT_LEFT).startActivityAsRoot(
                            MainActivity::class.java
                        )
                    }, 250)
                }
                res.isError() -> {
                    progressBar.gone()
                    dialogController.alert(
                        getString(R.string.error),
                        res.throwable()?.message!!
                    )
                }
                res.isLoading() -> {
                    progressBar.visible()
                }
            }
        })

        viewModel.progressLiveData.observe(this, Observer { progress ->
            progressBar.progress = progress
        })
    }

    private fun createAccount(
        phrase: Array<String?>? = null,
        authRequired: Boolean
    ) {
        val account = if (null == phrase) {
            Account()
        } else {
            Account.fromRecoveryPhrase(*phrase)
        }
        val spec = KeyAuthenticationSpec.Builder(this)
            //.setAuthenticationValidityDuration(BuildConfig.KEY_VALIDITY_DURATION)
            .setKeyAlias(account.accountNumber)
            .setAuthenticationRequired(authRequired).build()
        account.saveToKeyStore(this, spec, object : Callback0 {
            override fun onSuccess() {

                val requester = account.accountNumber
                val signingPrivateKey = account.keyPair.privateKey().toBytes()

                // ignore register encryption key in case of recover account
                var encPubKeyHex: String? = null
                var encPubKeySig: String? = null
                if (null == phrase) {
                    val encPubKey =
                        account.encryptionKey.publicKey().toBytes()
                    encPubKeyHex = HEX.encode(encPubKey)
                    encPubKeySig = HEX.encode(
                        Ed25519.sign(
                            encPubKey,
                            signingPrivateKey
                        )
                    )
                }

                val timestamp = System.currentTimeMillis().toString()
                val jwtSig = HEX.encode(
                    Ed25519.sign(
                        RAW.decode(timestamp),
                        signingPrivateKey
                    )
                )

                viewModel.registerAccount(
                    timestamp,
                    jwtSig,
                    encPubKeySig,
                    encPubKeyHex,
                    requester,
                    authRequired
                )
            }

            override fun onError(throwable: Throwable?) {
                when (throwable) {

                    // authentication error
                    is AuthenticationException -> {
                        // Do nothing
                    }

                    // missing security requirement
                    is AuthenticationRequiredException -> {
                        when (throwable.type) {

                            // did not set up fingerprint/biometric
                            AuthenticationRequiredException.FINGERPRINT, AuthenticationRequiredException.BIOMETRIC -> {
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
                        dialogController.alert(
                            getString(R.string.error),
                            throwable?.message
                                ?: getString(R.string.unexpected_error)
                        )
                    }
                }
            }

        })
    }

    private fun gotoSecuritySetting() {
        val intent = Intent(Settings.ACTION_SECURITY_SETTINGS)
        navigator.anim(BOTTOM_UP).startActivity(intent)
    }

    override fun onBackPressed() {
        navigator.anim(RIGHT_LEFT).finishActivity()
        super.onBackPressed()
    }

}