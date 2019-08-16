package com.bitmark.registry.util.extension

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.provider.Settings
import com.bitmark.registry.feature.Navigator
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.google.zxing.common.BitMatrix
import org.json.JSONArray
import java.io.IOException


/**
 * @author Hieu Pham
 * @since 2019-07-22
 * Email: hieupham@bitmark.com
 * Copyright © 2019 Bitmark. All rights reserved.
 */

inline fun <reified T> Gson.fromJson(json: String) =
    this.fromJson<T>(json, object : TypeToken<T>() {}.type)

inline fun <reified T> Gson.toJson(value: T) =
    this.toJson(value, object : TypeToken<T>() {}.type)

fun Map<String, String>.toJson() = Gson().toJson(this)

fun BitMatrix.toBitmap(size: Int): Bitmap {
    val pixels = IntArray(width * height)
    for (y in 0 until height) {
        val offset = y * width
        for (x in 0 until width) {
            pixels[offset + x] =
                if (get(x, y)) 0xFF000000.toInt() else 0xFFFFFFFF.toInt()
        }
    }
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    bitmap.setPixels(pixels, 0, size, 0, 0, width, height)
    return bitmap
}

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

fun JSONArray.toStringArray() = try {

    val array = Array(length()) { "" }
    for (i in (0 until length())) {
        array[i] = get(i).toString()
    }
    array

} catch (e: Throwable) {
    null
}

fun Throwable.isNetworkError() = this is IOException
