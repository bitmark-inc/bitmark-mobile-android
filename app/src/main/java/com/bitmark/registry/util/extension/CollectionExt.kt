package com.bitmark.registry.util.extension

import java.util.*
import kotlin.collections.ArrayList


/**
 * @author Hieu Pham
 * @since 2019-07-12
 * Email: hieupham@bitmark.com
 * Copyright © 2019 Bitmark. All rights reserved.
 */

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