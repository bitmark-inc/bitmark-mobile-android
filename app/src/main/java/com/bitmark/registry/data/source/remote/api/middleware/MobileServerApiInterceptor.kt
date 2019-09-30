package com.bitmark.registry.data.source.remote.api.middleware

import android.text.TextUtils
import com.bitmark.registry.logging.Tracer
import okhttp3.Response
import javax.inject.Inject


/**
 * @author Hieu Pham
 * @since 7/1/19
 * Email: hieupham@bitmark.com
 * Copyright © 2019 Bitmark. All rights reserved.
 */
class MobileServerApiInterceptor @Inject constructor() : Interceptor() {

    override fun getTag(): String? = "MobileServerApi"

    override fun intercept(chain: okhttp3.Interceptor.Chain): Response {
        val builder = chain.request().newBuilder()
            .addHeader("Content-Type", "application/json")
            .addHeader("Accept", "application/json")
            .addHeader("Cache-Control", "no-cache")
            .addHeader("Cache-Control", "no-store")
        if (!TextUtils.isEmpty(Cache.getInstance().mobileServerJwt))
            builder.addHeader(
                "Authorization",
                "Bearer " + Cache.getInstance().mobileServerJwt
            )

        val req = builder.build()
        Tracer.INFO.log(getTag()!!, req.toString())
        val res = chain.proceed(req)
        Tracer.INFO.log(getTag()!!, res.toString())
        return res
    }
}