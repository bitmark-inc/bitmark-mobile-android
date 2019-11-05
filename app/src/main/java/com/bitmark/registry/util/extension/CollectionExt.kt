/**
 * SPDX-License-Identifier: ISC
 * Copyright © 2014-2019 Bitmark. All rights reserved.
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 */
package com.bitmark.registry.util.extension

import java.util.*
import kotlin.collections.ArrayList

fun <T> MutableList<T>.append(vararg items: List<T>): List<T> {
    items.forEach { i -> addAll(i) }
    return this
}

fun <T> Queue<T>.poll(count: Int): List<T> {
    val result = ArrayList<T>(count)
    val loop = if (size < count) size else count
    for (i in 0 until loop) {
        result.add(poll())
    }
    return result
}