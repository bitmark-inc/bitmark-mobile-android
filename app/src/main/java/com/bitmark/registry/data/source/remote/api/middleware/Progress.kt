package com.bitmark.registry.data.source.remote.api.middleware


/**
 * @author Hieu Pham
 * @since 2019-07-29
 * Email: hieupham@bitmark.com
 * Copyright © 2019 Bitmark. All rights reserved.
 */
data class Progress(
    val identifier: String,
    val progress: Int,
    val done: Boolean
)