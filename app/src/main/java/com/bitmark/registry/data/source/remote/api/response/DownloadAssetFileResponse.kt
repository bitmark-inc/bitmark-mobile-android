package com.bitmark.registry.data.source.remote.api.response

import com.bitmark.registry.util.encryption.SessionData


/**
 * @author Hieu Pham
 * @since 2019-07-18
 * Email: hieupham@bitmark.com
 * Copyright © 2019 Bitmark. All rights reserved.
 */
class DownloadAssetFileResponse(
    val sessionData: SessionData,
    val fileName: String,
    val fileContent: ByteArray
) : Response