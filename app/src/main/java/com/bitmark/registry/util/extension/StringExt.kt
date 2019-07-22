package com.bitmark.registry.util.extension


/**
 * @author Hieu Pham
 * @since 2019-07-21
 * Email: hieupham@bitmark.com
 * Copyright © 2019 Bitmark. All rights reserved.
 */

fun String.shortenAccountNumber(): String {
    return "[%s...%s]".format(
        this.substring(0, 4),
        this.substring(length - 4, length)
    )
}