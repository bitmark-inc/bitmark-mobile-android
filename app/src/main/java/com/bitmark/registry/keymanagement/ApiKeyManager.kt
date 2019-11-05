/**
 * SPDX-License-Identifier: ISC
 * Copyright © 2014-2019 Bitmark. All rights reserved.
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 */
package com.bitmark.registry.keymanagement

internal class ApiKeyManager {

    val bitmarkApiKey: String
        external get

    val intercomApiKey: String
        external get


    companion object {

        val API_KEY_MANAGER = ApiKeyManager()

        init {
            System.loadLibrary("api-key")
        }
    }

}
