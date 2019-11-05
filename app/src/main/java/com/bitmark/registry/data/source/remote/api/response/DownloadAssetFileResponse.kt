/**
 * SPDX-License-Identifier: ISC
 * Copyright © 2014-2019 Bitmark. All rights reserved.
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 */
package com.bitmark.registry.data.source.remote.api.response

import com.bitmark.registry.util.encryption.SessionData

class DownloadAssetFileResponse(
    val sessionData: SessionData,
    val fileName: String,
    val fileContent: ByteArray
) : Response