package com.bitmark.registry.feature.transfer

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.widget.TextView
import androidx.core.widget.doOnTextChanged
import androidx.lifecycle.Observer
import com.bitmark.apiservice.params.TransferParams
import com.bitmark.apiservice.utils.Address
import com.bitmark.registry.R
import com.bitmark.registry.data.model.BitmarkData
import com.bitmark.registry.feature.BaseAppCompatActivity
import com.bitmark.registry.feature.BaseViewModel
import com.bitmark.registry.feature.DialogController
import com.bitmark.registry.feature.Navigator
import com.bitmark.registry.feature.Navigator.Companion.RIGHT_LEFT
import com.bitmark.registry.feature.register.RegisterContainerActivity
import com.bitmark.registry.feature.scan_qr_code.ScanQrCodeActivity
import com.bitmark.registry.logging.Event
import com.bitmark.registry.logging.EventLogger
import com.bitmark.registry.logging.Tracer
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

        private const val BITMARK = "bitmark"

        private const val TAG = "TransferActivity"

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

    @Inject
    lateinit var logger: EventLogger

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
                hideKeyBoard()
                transfer(bitmark, keyAlias, recipient)
            } else {
                showError()
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

        etRecipient.doOnTextChanged { text, _, _, _ ->
            if (text?.isNotBlank() == true) {
                ivClear.visible()
                btnTransfer.enable()
            } else {
                ivClear.invisible()
                btnTransfer.disable()
            }
        }

    }

    private fun showError() {
        tvError.visible()
        handler.postDelayed({
            tvError.invisible()
        }, 1000)
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
                    Tracer.ERROR.log(
                        TAG,
                        "transfer bitmark failed: ${res.throwable()
                            ?: "unknown"}"
                    )
                    logger.logError(Event.PROP_TRANSFER_ERROR, res.throwable())
                    blocked = false
                    progressBar.gone()
                    dialogController.alert(
                        R.string.error,
                        R.string.could_not_transfer
                    )
                }

                res.isLoading() -> {
                    blocked = true
                    val snackbar = Snackbar.make(
                        btnTransfer,
                        R.string.transferring_your_rights_three_dot,
                        Snackbar.LENGTH_SHORT
                    )
                    val view = snackbar.view
                    view.background =
                        getDrawable(R.drawable.bg_wild_sand_shadow)
                    view.findViewById<TextView>(com.google.android.material.R.id.snackbar_text)
                        ?.setTextColorRes(android.R.color.black)
                    snackbar.show()
                    progressBar.visible()
                }
            }
        })

        viewModel.transferProgressLiveData.observe(this, Observer { progress ->
            progressBar.progress = progress
            if (progress >= 100) {
                // delay a bit for visible to user
                handler.postDelayed({ progressBar.gone() }, 200)
            }
        })

        viewModel.getKeyAliasLiveData().observe(this, Observer { res ->
            when {
                res.isSuccess() -> {
                    keyAlias = res.data()!!
                }

                res.isError() -> {
                    Tracer.ERROR.log(
                        TAG,
                        "get key alias failed: ${res.throwable() ?: "unknown"}"
                    )
                    dialogController.alert(
                        R.string.error,
                        R.string.unexpected_error,
                        R.string.ok
                    ) { navigator.finishActivity() }
                }
            }
        })

        viewModel.bitmarkDeletedLiveData.observe(this, Observer { p ->
            if (bitmark.id != p.first) return@Observer
            when (p.second) {
                BitmarkData.Status.TO_BE_TRANSFERRED -> {
                    // do nothing since it's already handled
                }

                else -> {
                    navigator.anim(RIGHT_LEFT).finishActivity()
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
            keyAlias
        ) { account ->
            val encKeyPair = account.encKeyPair
            val params = TransferParams(
                Address.fromAccountNumber(recipient),
                bitmark.headId
            )
            params.sign(account.authKeyPair)
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
        action: (Account) -> Unit
    ) {
        val spec =
            KeyAuthenticationSpec.Builder(this).setKeyAlias(keyAlias)
                .setAuthenticationDescription(getString(R.string.your_authorization_is_required))
                .build()
        loadAccount(accountNumber,
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