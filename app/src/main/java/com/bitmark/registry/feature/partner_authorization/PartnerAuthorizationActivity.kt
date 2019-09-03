package com.bitmark.registry.feature.partner_authorization

import android.Manifest
import android.os.Bundle
import android.os.Handler
import androidx.lifecycle.Observer
import com.bitmark.registry.R
import com.bitmark.registry.feature.BaseAppCompatActivity
import com.bitmark.registry.feature.BaseViewModel
import com.bitmark.registry.feature.DialogController
import com.bitmark.registry.feature.Navigator
import com.bitmark.registry.feature.Navigator.Companion.RIGHT_LEFT
import com.bitmark.registry.feature.register.RegisterContainerActivity
import com.bitmark.registry.util.extension.*
import com.bitmark.registry.util.view.InfoAppCompatDialog
import com.bitmark.sdk.authentication.KeyAuthenticationSpec
import com.bitmark.sdk.features.Account
import com.google.zxing.ResultPoint
import com.journeyapps.barcodescanner.BarcodeCallback
import com.journeyapps.barcodescanner.BarcodeResult
import com.tbruyelle.rxpermissions2.RxPermissions
import io.reactivex.disposables.CompositeDisposable
import kotlinx.android.synthetic.main.activity_partner_authorization.*
import kotlinx.android.synthetic.main.activity_scan_qr_code.ivBack
import kotlinx.android.synthetic.main.activity_scan_qr_code.viewScanner
import javax.inject.Inject


/**
 * @author Hieu Pham
 * @since 2019-08-07
 * Email: hieupham@bitmark.com
 * Copyright © 2019 Bitmark. All rights reserved.
 */
class PartnerAuthorizationActivity : BaseAppCompatActivity() {

    @Inject
    lateinit var viewModel: PartnerAuthorizationViewModel

    @Inject
    lateinit var navigator: Navigator

    @Inject
    lateinit var dialogController: DialogController

    private val handler = Handler()

    private val compositeDisposable = CompositeDisposable()

    private var accountNumber: String? = null

    private var keyAlias: String? = null

    override fun layoutRes(): Int = R.layout.activity_partner_authorization

    override fun viewModel(): BaseViewModel? = viewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel.getAccountInfo()
    }

    override fun initComponents() {
        super.initComponents()

        // a bit delay for better ux
        handler.postDelayed({
            val rxPermission = RxPermissions(this)
            compositeDisposable.add(rxPermission.requestEach(Manifest.permission.CAMERA).subscribe { permission ->
                if (!permission.granted) {
                    if (permission.shouldShowRequestPermissionRationale) {
                        navigator.anim(RIGHT_LEFT).finishActivity()
                    } else {
                        dialogController.alert(
                            R.string.enable_camera_access,
                            R.string.to_get_started_allow_access_camera,
                            R.string.enable_access
                        ) {
                            navigator.openAppSetting(this)
                        }
                    }
                }
            })
        }, 250)

        ivBack.setOnClickListener {
            navigator.anim(RIGHT_LEFT).finishActivity()
        }

        viewScanner.decodeSingle(object : BarcodeCallback {
            override fun barcodeResult(result: BarcodeResult?) {
                if (result != null && result.text != null) {
                    try {
                        val text = result.text.split("|")
                        if (text.size != 2) throw IllegalStateException("unrecognized QR code")
                        val code = text[0]
                        val url = text[1]

                        dialogController.confirm(
                            getString(R.string.authorization_required),
                            getString(R.string.requires_your_digital_signature_format).format(
                                url.toHost()
                            ), false,
                            getString(R.string.authorize),
                            {
                                authorize(url, code)
                            },
                            getString(R.string.cancel),
                            {
                                navigator.anim(RIGHT_LEFT).finishActivity()
                            })


                    } catch (e: Throwable) {
                        dialogController.alert(
                            R.string.unrecognized_qr_code,
                            R.string.please_scan_the_qr_code_again
                        ) {
                            viewScanner.decodeSingle(this)
                        }
                    }
                }
            }

            override fun possibleResultPoints(resultPoints: MutableList<ResultPoint>?) {
            }

        })
    }

    private fun authorize(url: String, code: String) {
        if (accountNumber == null || keyAlias == null) return
        loadAccount(
            accountNumber!!,
            keyAlias!!
        ) { account ->
            viewModel.authorize(
                accountNumber!!,
                url,
                code,
                account.keyPair
            )
        }
    }

    private fun loadAccount(
        accountNumber: String,
        keyAlias: String,
        action: (Account) -> Unit
    ) {
        val spec = KeyAuthenticationSpec.Builder(this).setKeyAlias(keyAlias)
            .setAuthenticationDescription(getString(R.string.your_authorization_is_required))
            .setUsePossibleAlternativeAuthentication(true)
            .build()
        this.loadAccount(accountNumber,
            spec,
            dialogController,
            successAction = action,
            setupRequiredAction = { navigator.gotoSecuritySetting() },
            invalidErrorAction = {
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

    override fun deinitComponents() {
        compositeDisposable.dispose()
        handler.removeCallbacksAndMessages(null)
        super.deinitComponents()
    }

    override fun observe() {
        super.observe()

        viewModel.getAccountInfoLiveData().observe(this, Observer { res ->
            when {
                res.isSuccess() -> {
                    val info = res.data() ?: return@Observer
                    accountNumber = info.first
                    keyAlias = info.second
                }

                res.isError() -> {
                    dialogController.alert(
                        R.string.error,
                        R.string.unexpected_error
                    ) {
                        navigator.anim(
                            RIGHT_LEFT
                        ).finishActivity()
                    }
                }
            }
        })

        viewModel.authorizeLiveData().observe(this, Observer { res ->
            when {
                res.isSuccess() -> {
                    progressBar.gone()
                    val url = res.data() ?: ""
                    val infoDialog = InfoAppCompatDialog(
                        this,
                        getString(R.string.your_authorization_has_been_format).format(
                            url.toHost()
                        ),
                        getString(R.string.authorized)
                    )

                    dialogController.show(infoDialog)

                    handler.postDelayed(
                        {
                            dialogController.dismiss(infoDialog) {
                                dialogController.alert(
                                    "",
                                    getString(R.string.to_complete_process)
                                ) {
                                    navigator.anim(RIGHT_LEFT).finishActivity()
                                }
                            }
                        },
                        1500
                    )
                }

                res.isError() -> {
                    progressBar.gone()
                    dialogController.alert(
                        R.string.error,
                        R.string.could_not_send_your_authorization
                    ) {
                        navigator.anim(
                            RIGHT_LEFT
                        ).finishActivity()
                    }
                }

                res.isLoading() -> {
                    progressBar.visible()
                }
            }
        })
    }

    override fun onResume() {
        super.onResume()
        // a bit delay for animation finish
        handler.postDelayed({ viewScanner.resume() }, 250)
    }

    override fun onPause() {
        viewScanner.pause()
        super.onPause()
    }

    override fun onBackPressed() {
        navigator.anim(RIGHT_LEFT).finishActivity()
        super.onBackPressed()
    }
}