package com.bitmark.registry.feature.transfer

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import androidx.lifecycle.Observer
import com.bitmark.apiservice.params.TransferParams
import com.bitmark.apiservice.utils.Address
import com.bitmark.registry.R
import com.bitmark.registry.feature.BaseAppCompatActivity
import com.bitmark.registry.feature.BaseViewModel
import com.bitmark.registry.feature.DialogController
import com.bitmark.registry.feature.Navigator
import com.bitmark.registry.feature.Navigator.Companion.RIGHT_LEFT
import com.bitmark.registry.feature.scan_qr_code.ScanQrCodeActivity
import com.bitmark.registry.util.extension.*
import com.bitmark.registry.util.modelview.BitmarkModelView
import com.bitmark.registry.util.view.InfoAppCompatDialog
import com.bitmark.sdk.authentication.KeyAuthenticationSpec
import com.bitmark.sdk.features.Account
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.activity_transfer.*
import javax.inject.Inject


/**
 * @author Hieu Pham
 * @since 2019-07-20
 * Email: hieupham@bitmark.com
 * Copyright © 2019 Bitmark. All rights reserved.
 */
class TransferActivity : BaseAppCompatActivity() {

    companion object {
        const val BITMARK = "BITMARK"

        fun getBundle(bitmark: BitmarkModelView): Bundle {
            val bundle = Bundle()
            bundle.putParcelable(BITMARK, bitmark)
            return bundle
        }
    }

    @Inject
    lateinit var viewModel: TransferViewModel

    @Inject
    lateinit var dialogController: DialogController

    @Inject
    lateinit var navigator: Navigator

    private lateinit var bitmark: BitmarkModelView

    private var blocked = false

    private lateinit var keyAlias: String

    private val handler = Handler()

    override fun layoutRes(): Int = R.layout.activity_transfer

    override fun viewModel(): BaseViewModel? = viewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel.getKeyAlias()
    }

    override fun initComponents() {
        super.initComponents()

        bitmark = intent?.extras?.getParcelable(BITMARK) as BitmarkModelView

        tvToolbarTitle.text =
            if (bitmark.name.isNullOrBlank()) getString(R.string.transfer) else bitmark.name

        btnTransfer.setSafetyOnclickListener {
            if (blocked) return@setSafetyOnclickListener
            val recipient = etRecipient.text.toString()
            if (Address.isValidAccountNumber(recipient) && bitmark.accountNumber != recipient) {
                tvError.invisible()
                hideKeyBoard()
                transfer(bitmark, keyAlias, recipient)
            } else {
                tvError.visible()
            }

        }

        ivQrCode.setSafetyOnclickListener {
            navigator.anim(RIGHT_LEFT).startActivityForResult(
                ScanQrCodeActivity::class.java,
                ScanQrCodeActivity.REQUEST_CODE
            )
        }

        ivClear.setOnClickListener { etRecipient.text?.clear() }

        ivBack.setOnClickListener {
            navigator.anim(RIGHT_LEFT).finishActivity()
        }

    }

    override fun deinitComponents() {
        dialogController.dismiss()
        handler.removeCallbacksAndMessages(null)
        super.deinitComponents()
    }

    override fun observe() {
        super.observe()
        viewModel.transferLiveData().observe(this, Observer { res ->
            when {
                res.isSuccess() -> {
                    blocked = false
                    val dialog = InfoAppCompatDialog(
                        this,
                        getString(R.string.your_property_rights_has_been_transferred)
                    )
                    dialogController.show(dialog)

                    handler.postDelayed({
                        dialogController.dismiss(dialog) {
                            navigator.anim(
                                RIGHT_LEFT
                            ).finishActivity()
                        }
                    }, 1500)
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
                    Snackbar.make(
                        btnTransfer,
                        R.string.transferring_your_rights_three_dot,
                        Snackbar.LENGTH_SHORT
                    ).show()
                    progressBar.visible()
                }
            }
        })

        viewModel.transferProgressLiveData.observe(this, Observer { progress ->
            progressBar.progress = progress
            if (progress >= 100) {
                progressBar.gone()
            }
        })

        viewModel.getKeyAliasLiveData().observe(this, Observer { res ->
            when {
                res.isSuccess() -> {
                    keyAlias = res.data()!!
                }

                res.isError() -> {
                    dialogController.alert(
                        R.string.error,
                        R.string.unexpected_error,
                        R.string.ok
                    ) { navigator.finishActivity() }
                }
            }
        })
    }

    private fun transfer(
        bitmark: BitmarkModelView,
        keyAlias: String,
        recipient: String
    ) {
        loadAccount(
            bitmark.accountNumber,
            keyAlias,
            getString(R.string.please_sign_to_transfer_bitmark)
        ) { account ->
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

    private fun loadAccount(
        accountNumber: String,
        keyAlias: String,
        message: String,
        action: (Account) -> Unit
    ) {
        val spec =
            KeyAuthenticationSpec.Builder(this).setKeyAlias(keyAlias)
                .setAuthenticationDescription(message)
                .build()
        loadAccount(accountNumber,
            spec,
            dialogController,
            successAction = action,
            setupRequiredAction = { navigator.gotoSecuritySetting() },
            unknownErrorAction = {
                dialogController.alert(
                    R.string.error,
                    R.string.unexpected_error
                )
            })
    }

    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    ) {
        when (requestCode) {
            ScanQrCodeActivity.REQUEST_CODE -> {
                if (resultCode == Activity.RESULT_OK) {
                    val text = data?.getStringExtra(ScanQrCodeActivity.RESULT)
                        ?: return
                    etRecipient.setText(text)
                }
            }
            else -> {
                super.onActivityResult(requestCode, resultCode, data)
            }
        }
    }

    override fun onBackPressed() {
        navigator.anim(RIGHT_LEFT).finishActivity()
        super.onBackPressed()
    }

}