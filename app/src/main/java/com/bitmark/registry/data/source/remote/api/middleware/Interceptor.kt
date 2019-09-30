package com.bitmark.registry.data.source.remote.api.middleware

import com.bitmark.registry.logging.Tracer
import okhttp3.Interceptor
import okhttp3.Response


/**
 * @author Hieu Pham
 * @since 7/1/19
 * Email: hieupham@bitmark.com
 * Copyright © 2019 Bitmark. All rights reserved.
 */
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