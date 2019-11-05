/**
 * SPDX-License-Identifier: ISC
 * Copyright © 2014-2019 Bitmark. All rights reserved.
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 */
package com.bitmark.registry.util.view

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import androidx.core.content.FileProvider
import com.bitmark.registry.BuildConfig
import com.bitmark.registry.R
import com.bitmark.registry.feature.Navigator
import com.bitmark.registry.util.extension.getDimensionPixelSize
import com.bitmark.registry.util.extension.setSafetyOnclickListener
import com.bitmark.registry.util.extension.toBitmap
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import io.reactivex.Completable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.layout_qr_code_sharing.*
import java.io.File
import java.io.FileOutputStream

class QrCodeSharingDialog(context: Context, private val text: String) :
    BaseBottomSheetDialog(context) {

    private var disposable: Disposable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setCanceledOnTouchOutside(false)
    }

    override fun layoutRes(): Int = R.layout.layout_qr_code_sharing

    override fun initComponents() {
        super.initComponents()

        disposable =
            generateQrCode(text).subscribe({ ivQrCode.setImageURI(getQrCodeUri()) },
                {})

        tvText.text = text

        ivClose.setOnClickListener { dismiss() }

        btnShare.setSafetyOnclickListener {
            dismiss()
            val intent = Intent(Intent.ACTION_SEND)
            intent.type = "*/*"
            intent.putExtra(Intent.EXTRA_SUBJECT, text)
            intent.putExtra(Intent.EXTRA_TEXT, text)
            intent.putExtra(Intent.EXTRA_STREAM, getQrCodeUri())
            Navigator(ownerActivity).anim(Navigator.BOTTOM_UP)
                .startActivity(Intent.createChooser(intent, text))
        }
    }

    private fun generateQrCode(text: String) = Completable.create { emt ->
        try {

            val file = getQrCodeFile()
            if (file.length() > 0) emt.onComplete()
            else {
                val writer = QRCodeWriter()
                val size = context.getDimensionPixelSize(R.dimen.dp_220)
                val bitMatrix =
                    writer.encode(text, BarcodeFormat.QR_CODE, size, size)
                val bitmap = bitMatrix.toBitmap(size)


                val os = FileOutputStream(file)
                os.use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
                emt.onComplete()
            }

        } catch (e: Throwable) {
            emt.onError(e)
        }
    }.subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())

    private fun getQrCodeUri() = FileProvider.getUriForFile(
        context,
        BuildConfig.APPLICATION_ID + ".file_provider",
        getQrCodeFile()
    )

    private fun getQrCodeFile(): File {
        val dir = File(context.filesDir, "account/qrcode")
        if (!dir.exists()) dir.mkdirs()
        val file = File(dir, "qr_code.png")
        if (!file.exists()) file.createNewFile()
        return file
    }

    override fun onDetachedFromWindow() {
        disposable?.dispose()
        super.onDetachedFromWindow()
    }
}