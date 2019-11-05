/**
 * SPDX-License-Identifier: ISC
 * Copyright © 2014-2019 Bitmark. All rights reserved.
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 */
package com.bitmark.registry.data.model

enum class Head(val value: String) {
    HEAD("head"), MOVED("moved"), PRIOR("prior"),
}

fun mapHead(head: String): Head? = when (head) {
    "head" -> Head.HEAD
    "moved" -> Head.MOVED
    "prior" -> Head.MOVED
    else -> null
}

fun mapHead(head: com.bitmark.apiservice.utils.record.Head?): Head? =
    when (head) {
        com.bitmark.apiservice.utils.record.Head.HEAD -> Head.HEAD
        com.bitmark.apiservice.utils.record.Head.MOVED -> Head.MOVED
        com.bitmark.apiservice.utils.record.Head.PRIOR -> Head.PRIOR
        else -> null
    }