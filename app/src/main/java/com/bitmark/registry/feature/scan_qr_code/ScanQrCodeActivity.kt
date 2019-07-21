package com.bitmark.registry.feature.scan_qr_code

import android.app.Activity
import android.content.Intent
import android.os.Handler
import com.bitmark.registry.R
import com.bitmark.registry.feature.BaseAppCompatActivity
import com.bitmark.registry.feature.BaseViewModel
import com.bitmark.registry.feature.Navigator
import com.bitmark.registry.feature.Navigator.Companion.RIGHT_LEFT
import com.google.zxing.ResultPoint
import com.journeyapps.barcodescanner.BarcodeCallback
import com.journeyapps.barcodescanner.BarcodeResult
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
    lateinit var navigator: Navigator<ScanQrCodeActivity>

    override fun layoutRes(): Int = R.layout.activity_scan_qr_code

    override fun viewModel(): BaseViewModel? = null

    override fun initComponents() {
        super.initComponents()

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

    override fun onResume() {
        super.onResume()
        // a bit delay for animation finish
        Handler().postDelayed({ viewScanner.resume() }, 250)
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