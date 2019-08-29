package com.bitmark.registry.util.extension

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import android.provider.Settings
import com.bitmark.registry.R
import com.bitmark.registry.feature.Navigator
import com.bitmark.registry.feature.Navigator.Companion.NONE


/**
 * @author Hieu Pham
 * @since 2019-08-21
 * Email: hieupham@bitmark.com
 * Copyright © 2019 Bitmark. All rights reserved.
 */

fun Navigator.gotoSecuritySetting() {
    val intent = Intent(Settings.ACTION_SECURITY_SETTINGS)
    anim(Navigator.BOTTOM_UP).startActivity(intent)
}

fun Navigator.openBrowser(url: String) {
    try {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        startActivity(intent)
    } catch (ignore: Throwable) {

    }
}

fun Navigator.openAppSetting(context: Context) {
    try {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        val uri = Uri.fromParts("package", context.packageName, null)
        intent.data = uri
        startActivity(intent)
    } catch (ignore: Throwable) {
    }
}

fun Navigator.openMail(context: Context, email: String) {
    try {
        val intent =
            Intent(Intent.ACTION_SENDTO, Uri.fromParts("mailto", email, null))
        intent.putExtra(Intent.EXTRA_EMAIL, arrayOf(email))
        startActivity(
            Intent.createChooser(
                intent,
                context.getString(R.string.send_to_format).format(email)
            )
        )
    } catch (ignore: Throwable) {
    }
}

fun Navigator.browseMedia(mime: String, requestCode: Int) {
    val intent = Intent(Intent.ACTION_PICK)
    when (mime) {
        "image/*" -> {
            intent.setDataAndType(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                mime
            )
        }

        "video/*" -> {
            intent.setDataAndType(
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                mime
            )
        }
    }
    anim(NONE).startActivityForResult(intent, requestCode)
}

fun Navigator.browseDocument(requestCode: Int) {
    val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
    intent.addCategory(Intent.CATEGORY_OPENABLE)
    intent.type = "*/*"
    anim(NONE).startActivityForResult(intent, requestCode)
}