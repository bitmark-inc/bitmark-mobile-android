/**
 * SPDX-License-Identifier: ISC
 * Copyright © 2014-2019 Bitmark. All rights reserved.
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 */
package com.bitmark.registry.data.source.remote.api.middleware

import com.bitmark.registry.logging.Tracer
import okhttp3.Interceptor
import okhttp3.Response

abstract class Interceptor : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val req = chain.request()
        if (getTag() != null) {
            Tracer.INFO.log(getTag()!!, req.toString())
        }
        val res = chain.proceed(req)
        if (getTag() != null) {
            Tracer.INFO.log(getTag()!!, res.toString())
        }
        return res
    }

    abstract fun getTag(): String?
}