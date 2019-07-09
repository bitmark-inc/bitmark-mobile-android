package com.bitmark.registry.util.extension

import android.os.Handler
import android.os.Looper
import androidx.lifecycle.MutableLiveData


/**
 * @author Hieu Pham
 * @since 2019-07-09
 * Email: hieupham@bitmark.com
 * Copyright © 2019 Bitmark. All rights reserved.
 */

fun <T> MutableLiveData<T>.set(value: T) {
    Handler(Looper.getMainLooper()).post { this.value = value }
}