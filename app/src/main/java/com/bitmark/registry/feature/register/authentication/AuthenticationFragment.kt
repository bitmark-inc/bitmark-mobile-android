package com.bitmark.registry.feature.register.authentication

import android.content.Context
import android.content.Intent
import android.hardware.fingerprint.FingerprintManager
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import androidx.core.hardware.fingerprint.FingerprintManagerCompat
import androidx.lifecycle.Observer
import com.bitmark.apiservice.utils.callback.Callback0
import com.bitmark.cryptography.crypto.Ed25519
import com.bitmark.cryptography.crypto.encoder.Hex.HEX
import com.bitmark.cryptography.crypto.encoder.Raw.RAW
import com.bitmark.registry.R
import com.bitmark.registry.feature.BaseSupportFragment
import com.bitmark.registry.feature.BaseViewModel
import com.bitmark.registry.feature.DialogController
import com.bitmark.registry.feature.Navigator
import com.bitmark.registry.feature.Navigator.Companion.RIGHT_LEFT
import com.bitmark.registry.feature.cloud_service_sign_in.CloudServiceSignInActivity
import com.bitmark.registry.feature.main.MainActivity
import com.bitmark.registry.util.extension.gone
import com.bitmark.registry.util.extension.gotoSecuritySetting
import com.bitmark.registry.util.extension.setSafetyOnclickListener
import com.bitmark.registry.util.extension.visible
import com.bitmark.sdk.authentication.KeyAuthenticationSpec
import com.bitmark.sdk.authentication.Provider
import com.bitmark.sdk.authentication.error.AuthenticationException
import com.bitmark.sdk.authentication.error.AuthenticationRequiredException
import com.bitmark.sdk.features.Account
import com.google.firebase.iid.FirebaseInstanceId
import kotlinx.android.synthetic.main.fragment_authentication.*
import javax.inject.Inject


/**
 * @author Hieu Pham
 * @since 7/5/19
 * Email: hieupham@bitmark.com
 * Copyright © 2019 Bitmark. All rights reserved.
 */
class AuthenticationFragment : BaseSupportFragment() {

    companion object {
        private const val RECOVERY_PHRASE = "recovery_phrase"

        private const val URI = "uri"

        private const val RECOVER_ACCOUNT = "recover_account"

        fun newInstance(
            phrase: Array<String?>? = null,
            uri: String? = null,
            recoverAccount: Boolean = false
        ): AuthenticationFragment {
            val fragment = AuthenticationFragment()
            val bundle = Bundle()
            if (null != phrase) bundle.putStringArray(RECOVERY_PHRASE, phrase)
            if (null != uri) bundle.putString(URI, uri)
            bundle.putBoolean(RECOVER_ACCOUNT, recoverAccount)
            fragment.arguments = bundle
            return fragment
        }
    }

    @Inject
    internal lateinit var viewModel: AuthenticationViewModel

    @Inject
    internal lateinit var dialogController: DialogController

    @Inject
    internal lateinit var navigator: Navigator

    private var blocked = false

    private val handler = Handler()

    override fun layoutRes(): Int = R.layout.fragment_authentication

    override fun viewModel(): BaseViewModel? = viewModel

    override fun initComponents() {
        super.initComponents()

        val phrase = arguments?.getStringArray(RECOVERY_PHRASE)
        val recoverAccount = arguments?.getBoolean(RECOVER_ACCOUNT) ?: false

        if (isBiometricSupported(context!!)) {
            ivFingerprint.visible()
            ivFace.visible()
            ivLock.gone()
            tvAuthType.setText(R.string.biometric_pin)
            tvDes.setText(R.string.enable_biometric_pin_to_secure)
            btnEnableAuth.setText(R.string.enable_biometric_pin)
        } else {
            ivFingerprint.gone()
            ivFace.gone()
            ivLock.visible()
            tvAuthType.setText(R.string.pin_password_pattern)
            tvDes.setText(R.string.enable_pin_password_pattern_to_secure)
            btnEnableAuth.setText(R.string.enable_pin_password_pattern)
        }

        btnEnableAuth.setSafetyOnclickListener {
            if (blocked) return@setSafetyOnclickListener
            if (recoverAccount && phrase != null) {
                recoverAccount(phrase, true)
            } else {
                createAccount(phrase, true)
            }

        }

        btnSkip.setSafetyOnclickListener {
            if (blocked) return@setSafetyOnclickListener
            if (recoverAccount && phrase != null) {
                recoverAccount(phrase, false)
            } else {
                createAccount(phrase, false)
            }
        }
    }

    override fun deinitComponents() {
        handler.removeCallbacksAndMessages(null)
        dialogController.dismiss()
        super.deinitComponents()
    }

    override fun observe() {
        super.observe()
        viewModel.registerAccountLiveData().observe(this, Observer { res ->
            when {
                res.isSuccess() -> {
                    handler.postDelayed({
                        val intent = Intent(
                            context,
                            CloudServiceSignInActivity::class.java
                        )
                        val bundle = CloudServiceSignInActivity.getBundle(true)
                        intent.putExtras(bundle)
                        val uri = arguments?.getString(URI)
                        if (uri != null) intent.data = Uri.parse(uri)
                        navigator.anim(RIGHT_LEFT).startActivityAsRoot(intent)
                    }, 250)
                    blocked = false
                }
                res.isError() -> {
                    progressBar.gone()
                    dialogController.alert(
                        R.string.error,
                        R.string.could_not_register_account
                    )
                    blocked = false
                }
                res.isLoading() -> {
                    blocked = true
                    progressBar.visible()
                }
            }
        })

        viewModel.progressLiveData.observe(this, Observer { progress ->
            progressBar.progress = progress
            if (progress >= 100) {
                progressBar.gone()
            }
        })

        viewModel.updateAccountLiveData().observe(this, Observer { res ->
            when {
                res.isSuccess() -> {
                    val intent = Intent(context, MainActivity::class.java)
                    val uri = arguments?.getString(URI)
                    if (uri != null) intent.data = Uri.parse(uri)
                    navigator.anim(RIGHT_LEFT).startActivityAsRoot(intent)
                    blocked = false
                }

                res.isError() -> {
                    dialogController.alert(
                        R.string.error,
                        R.string.could_not_recover_account
                    )
                    blocked = false
                }

                res.isLoading() -> {
                    blocked = true
                }
            }
        })
    }

    private fun recoverAccount(phrase: Array<String?>, authRequired: Boolean) {
        val account = Account.fromRecoveryPhrase(*phrase)

        saveAccount(account, authRequired) { keyAlias ->
            viewModel.updateAccount(
                account.authKeyPair,
                account.accountNumber,
                authRequired,
                keyAlias
            )
        }
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

        saveAccount(account, authRequired) { keyAlias ->
            val requester = account.accountNumber
            val signingKeyPair = account.authKeyPair
            val signingPrivateKey = signingKeyPair.privateKey().toBytes()

            // ignore register encryption key in case of recover account
            var encPubKeyHex: String? = null
            var encPubKeySig: String? = null
            if (null == phrase) {
                val encPubKey =
                    account.encKeyPair.publicKey().toBytes()
                encPubKeyHex = HEX.encode(encPubKey)
                encPubKeySig = HEX.encode(
                    Ed25519.sign(
                        encPubKey,
                        signingPrivateKey
                    )
                )
            }

            val timestamp = System.currentTimeMillis().toString()
            val mobileServerSig = HEX.encode(
                Ed25519.sign(
                    RAW.decode(timestamp),
                    signingPrivateKey
                )
            )

            getFirebaseToken { token ->
                viewModel.registerAccount(
                    timestamp,
                    mobileServerSig,
                    encPubKeySig,
                    encPubKeyHex,
                    requester,
                    authRequired,
                    keyAlias,
                    token,
                    signingKeyPair
                )
            }
        }
    }

    private fun saveAccount(
        account: Account,
        authRequired: Boolean,
        successAction: (String) -> Unit
    ) {
        val keyAlias =
            "%s.%d.encryption_key".format(
                account.accountNumber,
                System.currentTimeMillis()
            )
        val accountAuthBuilder = KeyAuthenticationSpec.Builder(context)
            .setKeyAlias(keyAlias)
            .setAuthenticationDescription(getString(R.string.your_authorization_is_required))
            .setAuthenticationRequired(authRequired)

        if (!isBiometricSupported(context!!)) {
            // use PIN/Password/Pattern as alternative authentication if the device does not support biometric
            accountAuthBuilder.setUseAlternativeAuthentication(true)
        }

        account.saveToKeyStore(
            activity,
            accountAuthBuilder.build(),
            object : Callback0 {
                override fun onSuccess() {
                    successAction(keyAlias)
                }

                override fun onError(throwable: Throwable?) {
                    when (throwable) {

                        // authentication error
                        is AuthenticationException -> {
                            // Do nothing
                        }

                        // missing security requirement
                        is AuthenticationRequiredException -> {
                            when (throwable.provider) {

                                // did not set up fingerprint/biometric
                                Provider.FINGERPRINT, Provider.BIOMETRIC -> {
                                    dialogController.alert(
                                        R.string.error,
                                        R.string.fingerprint_required
                                    ) { navigator.gotoSecuritySetting() }
                                }

                                // did not set up pass code
                                else -> {
                                    dialogController.alert(
                                        R.string.error,
                                        R.string.passcode_pin_required
                                    ) { navigator.gotoSecuritySetting() }
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

    override fun onBackPressed() = navigator.popFragment() ?: false

    private fun getFirebaseToken(action: (String?) -> Unit) {
        FirebaseInstanceId.getInstance()
            .instanceId.addOnCompleteListener { task ->
            if (!task.isSuccessful) action.invoke(null)
            else action.invoke(task.result?.token)
        }
    }

    private fun isBiometricSupported(context: Context) =
        FingerprintManagerCompat.from(context).isHardwareDetected || (context.getSystemService(
            Context.FINGERPRINT_SERVICE
        ) as? FingerprintManager)?.isHardwareDetected ?: false

}