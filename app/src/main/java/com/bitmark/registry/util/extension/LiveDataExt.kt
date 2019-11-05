/**
 * SPDX-License-Identifier: ISC
 * Copyright © 2014-2019 Bitmark. All rights reserved.
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 */
package com.bitmark.registry.util.extension

import android.os.Handler
import android.os.Looper
import androidx.lifecycle.MutableLiveData
import com.bitmark.registry.util.livedata.BufferedLiveData

fun <T> MutableLiveData<T>.set(value: T) {
    if (Looper.myLooper() != Looper.getMainLooper()) {
        Handler(Looper.getMainLooper()).post { this.value = value }
    } else {
        this.value = value
    }
}

fun <T> BufferedLiveData<T>.set(value: T) {
    if (Looper.myLooper() != Looper.getMainLooper()) {
        Handler(Looper.getMainLooper()).post { setValue(value) }
    } else {
        setValue(value)
    }
}