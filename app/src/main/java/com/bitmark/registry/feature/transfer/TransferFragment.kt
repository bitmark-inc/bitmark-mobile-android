package com.bitmark.registry.feature.transfer

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.provider.Settings
import androidx.lifecycle.Observer
import com.bitmark.apiservice.params.TransferParams
import com.bitmark.apiservice.utils.Address
import com.bitmark.apiservice.utils.callback.Callback1
import com.bitmark.registry.R
import com.bitmark.registry.feature.BaseSupportFragment
import com.bitmark.registry.feature.BaseViewModel
import com.bitmark.registry.feature.DialogController
import com.bitmark.registry.feature.Navigator
import com.bitmark.registry.util.extension.*
import com.bitmark.registry.util.modelview.BitmarkModelView
import com.bitmark.sdk.authentication.KeyAuthenticationSpec
import com.bitmark.sdk.authentication.error.AuthenticationException
import com.bitmark.sdk.authentication.error.AuthenticationRequiredException
import com.bitmark.sdk.features.Account
import kotlinx.android.synthetic.main.fragment_transfer.*
import javax.inject.Inject


/**
 * @author Hieu Pham
 * @since 2019-07-20
 * Email: hieupham@bitmark.com
 * Copyright © 2019 Bitmark. All rights reserved.
 */
class TransferFragment : BaseSupportFragment() {

    companion object {
        const val BITMARK = "BITMARK"

        fun newInstance(bitmark: BitmarkModelView): TransferFragment {
            val bundle = Bundle()
            bundle.putParcelable(BITMARK, bitmark)
            val fragment = TransferFragment()
            fragment.arguments = bundle
            return fragment
        }
    }

    @Inject
    lateinit var viewModel: TransferViewModel

    @Inject
    lateinit var dialogController: DialogController

    @Inject
    lateinit var navigator: Navigator<TransferFragment>

    private lateinit var bitmark: BitmarkModelView

    private var blocked = false

    override fun layoutRes(): Int = R.layout.fragment_transfer

    override fun viewModel(): BaseViewModel? = viewModel

    override fun initComponents() {
        super.initComponents()

        bitmark = arguments?.getParcelable(BITMARK)!!

        tvToolbarTitle.text =
            if (bitmark.name.isNullOrBlank()) getString(R.string.transfer) else bitmark.name

        btnTransfer.setSafetyOnclickListener {
            if (blocked) return@setSafetyOnclickListener
            val recipient = etRecipient.text.toString()
            if (Address.isValidAccountNumber(recipient) && bitmark.accountNumber != recipient) {
                tvError.invisible()
                activity?.hideKeyBoard()
                transfer(bitmark, recipient)
            } else {
                tvError.visible()
            }

        }

        ivQrCode.setSafetyOnclickListener { }

        ivClear.setOnClickListener { etRecipient.text?.clear() }

        ivBack.setOnClickListener { navigator.popFragment() }

    }

    override fun observe() {
        super.observe()
        viewModel.transferLiveData().observe(this, Observer { res ->
            when {
                res.isSuccess() -> {
                    blocked = false
                    progressBar.gone()
                    Handler().postDelayed({
                        navigator.anim(Navigator.RIGHT_LEFT).finishActivity()
                    }, 200)
                }

                res.isError() -> {
                    blocked = false
                    progressBar.gone()
                    dialogController.alert(
                        R.string.error,
                        R.string.could_not_transfer
                    )
                }

                res.isLoading() -> {
                    blocked = true
                    progressBar.visible()
                }
            }
        })

        viewModel.transferProgressLiveData.observe(this, Observer { progress ->
            progressBar.progress = progress
        })
    }

    private fun transfer(bitmark: BitmarkModelView, recipient: String) {
        loadAccount(getString(R.string.please_sign_to_transfer_bitmark)) { account ->
            val encKeyPair = account.encryptionKey
            val params = TransferParams(
                Address.fromAccountNumber(recipient),
                bitmark.headId
            )
            params.sign(account.keyPair)
            viewModel.transfer(
                params,
                bitmark.assetId,
                bitmark.id,
                bitmark.accountNumber,
                encKeyPair
            )
        }
    }

    private fun loadAccount(message: String, action: (Account) -> Unit) {
        val accountNumber = bitmark.accountNumber
        val spec =
            KeyAuthenticationSpec.Builder(context).setKeyAlias(accountNumber)
                .setAuthenticationDescription(message)
                .build()
        Account.loadFromKeyStore(
            activity,
            accountNumber,
            spec,
            object : Callback1<Account> {
                override fun onSuccess(account: Account?) {
                    if (account == null) return
                    action.invoke(account)
                }

                override fun onError(throwable: Throwable?) {
                    when (throwable) {

                        is AuthenticationException -> {
                            // do nothing
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
                                R.string.error,
                                R.string.unexpected_error
                            )
                        }
                    }
                }

            })
    }

    private fun gotoSecuritySetting() {
        val intent = Intent(Settings.ACTION_SECURITY_SETTINGS)
        navigator.startActivity(intent)
    }
}