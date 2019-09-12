package com.bitmark.registry.data.source.remote.api.middleware

import com.bitmark.registry.logging.Tracer
import okhttp3.Response


/**
 * @author Hieu Pham
 * @since 7/1/19
 * Email: hieupham@bitmark.com
 * Copyright © 2019 Bitmark. All rights reserved.
 */
class CoreApiInterceptor : Interceptor() {

    override fun intercept(chain: okhttp3.Interceptor.Chain): Response {
        Tracer.INFO.log("CoreApiInterceptor", chain.request().toString())
        return super.intercept(chain)
    }
}