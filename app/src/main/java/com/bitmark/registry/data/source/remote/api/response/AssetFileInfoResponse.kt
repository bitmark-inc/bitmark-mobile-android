package com.bitmark.registry.data.source.remote.api.response

import com.bitmark.registry.util.encryption.SessionData


/**
 * @author Hieu Pham
 * @since 2019-07-20
 * Email: hieupham@bitmark.com
 * Copyright © 2019 Bitmark. All rights reserved.
 */
data class AssetFileInfoResponse(
    val sessionData: SessionData?,
    val orgContentType: String?,
    val expiration: String?,
    val name: String?,
    val date: String?
) : Response {
    companion object {
        fun newInstance() = AssetFileInfoResponse(null, null, null, null, null)
    }
}