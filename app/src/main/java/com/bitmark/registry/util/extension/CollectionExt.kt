package com.bitmark.registry.util.extension


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