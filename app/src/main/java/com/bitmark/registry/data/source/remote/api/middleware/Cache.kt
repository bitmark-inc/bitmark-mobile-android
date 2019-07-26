package com.bitmark.registry.data.source.remote.api.middleware


/**
 * @author Hieu Pham
 * @since 7/4/19
 * Email: hieupham@bitmark.com
 * Copyright © 2019 Bitmark. All rights reserved.
 */
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

    fun clear() {
        mobileServerJwt = ""
    }
}