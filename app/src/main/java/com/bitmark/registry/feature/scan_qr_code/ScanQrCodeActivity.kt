package com.bitmark.registry.feature.scan_qr_code

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.os.Handler
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.ImageSpan
import com.bitmark.registry.R
import com.bitmark.registry.feature.BaseAppCompatActivity
import com.bitmark.registry.feature.BaseViewModel
import com.bitmark.registry.feature.DialogController
import com.bitmark.registry.feature.Navigator
import com.bitmark.registry.feature.Navigator.Companion.RIGHT_LEFT
import com.bitmark.registry.util.extension.openAppSetting
import com.google.zxing.ResultPoint
import com.journeyapps.barcodescanner.BarcodeCallback
import com.journeyapps.barcodescanner.BarcodeResult
import com.tbruyelle.rxpermissions2.RxPermissions
import io.reactivex.disposables.CompositeDisposable
import kotlinx.android.synthetic.main.activity_scan_qr_code.*
import javax.inject.Inject


/**
 * @author Hieu Pham
 * @since 2019-07-21
 * Email: hieupham@bitmark.com
 * Copyright © 2019 Bitmark. All rights reserved.
 */
class ScanQrCodeActivity : BaseAppCompatActivity() {

    companion object {
        const val REQUEST_CODE = 0xAF

        const val RESULT = "RESULT"
    }

    @Inject
    lateinit var navigator: Navigator

    @Inject
    lateinit var dialogController: DialogController

    private val compositeDisposable = CompositeDisposable()

    private val handler = Handler()

    override fun layoutRes(): Int = R.layout.activity_scan_qr_code

    override fun viewModel(): BaseViewModel? = null

    override fun initComponents() {
        super.initComponents()

        val imageSpan =
            ImageSpan(this, R.drawable.ic_qr_code_2, ImageSpan.ALIGN_BOTTOM)
        val text = getString(R.string.you_can_transfer_rights_to_another)
        val startSpanIndex = text.indexOf("@")
        val endSpanIndex = startSpanIndex + 1
        val spanStringBuilder = SpannableStringBuilder(text)
        spanStringBuilder.setSpan(
            imageSpan,
            startSpanIndex,
            endSpanIndex,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        tvMessage.text = spanStringBuilder

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
                    val intent = Intent()
                    intent.putExtra(RESULT, result.text)
                    setResult(Activity.RESULT_OK, intent)
                    navigator.anim(RIGHT_LEFT).finishActivity()
                }
            }

            override fun possibleResultPoints(resultPoints: MutableList<ResultPoint>?) {
            }

        })
    }

    override fun deinitComponents() {
        compositeDisposable.dispose()
        handler.removeCallbacksAndMessages(null)
        super.deinitComponents()
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