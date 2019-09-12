package com.bitmark.registry.data.source.remote.api.middleware

import android.text.TextUtils
import com.bitmark.registry.logging.Tracer
import okhttp3.Response


/**
 * @author Hieu Pham
 * @since 7/1/19
 * Email: hieupham@bitmark.com
 * Copyright © 2019 Bitmark. All rights reserved.
 */
class MobileServerApiInterceptor : Interceptor() {

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
        Tracer.INFO.log("MobileServerApiInterceptor", req.toString())
        return chain.proceed(req)
    }
}