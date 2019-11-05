/**
 * SPDX-License-Identifier: ISC
 * Copyright © 2014-2019 Bitmark. All rights reserved.
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 */
package com.bitmark.registry.data.source.remote.api.middleware

class Cache private constructor() {

    companion object {

        @Volatile
        private var INSTANCE: Cache? = null

        fun getInstance(): Cache {
            if (null == INSTANCE) {
                synchronized(Cache::class) {
                    if (null == INSTANCE) {
                        INSTANCE = Cache()
                    }
                }
            }
            return INSTANCE!!
        }
    }

    var mobileServerJwt: String = ""

    var expiresAt: Long = -1L

    fun clear() {
        mobileServerJwt = ""
        expiresAt = -1L
    }
}