/**
 * SPDX-License-Identifier: ISC
 * Copyright © 2014-2019 Bitmark. All rights reserved.
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 */
package com.bitmark.registry.util.extension

import java.net.URI

fun String.shortenAccountNumber(): String {
    return "[%s...%s]".format(
        this.substring(0, 4),
        this.substring(length - 4, length)
    )
}

fun String.toHost(): String {
    return try {
        val uri = URI(this)
        uri.host
    } catch (e: Throwable) {
        this
    }
}