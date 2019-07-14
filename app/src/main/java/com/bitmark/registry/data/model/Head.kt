package com.bitmark.registry.data.model


/**
 * @author Hieu Pham
 * @since 2019-07-10
 * Email: hieupham@bitmark.com
 * Copyright © 2019 Bitmark. All rights reserved.
 */
enum class Head(val value: String) {
    HEAD("head"), MOVED("moved"), PRIOR("prior")

}

fun mapHead(head: com.bitmark.apiservice.utils.record.Head?): Head? =
    when (head) {
        com.bitmark.apiservice.utils.record.Head.HEAD -> Head.HEAD
        com.bitmark.apiservice.utils.record.Head.MOVED -> Head.MOVED
        com.bitmark.apiservice.utils.record.Head.PRIOR -> Head.PRIOR
        else -> null
    }