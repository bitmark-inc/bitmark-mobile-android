package com.bitmark.registry.data.source.remote.api.error


/**
 * @author Hieu Pham
 * @since 2019-08-27
 * Email: hieupham@bitmark.com
 * Copyright © 2019 Bitmark. All rights reserved.
 */
class NetworkException(cause: Throwable) : Exception(cause) {
    override val message: String?
        get() = "Network error"
}