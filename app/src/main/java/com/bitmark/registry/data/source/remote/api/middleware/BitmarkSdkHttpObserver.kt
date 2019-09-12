package com.bitmark.registry.data.source.remote.api.middleware

import com.bitmark.apiservice.middleware.HttpObserver
import com.bitmark.registry.logging.Tracer
import okhttp3.Request
import okhttp3.Response
import javax.inject.Inject


/**
 * @author Hieu Pham
 * @since 2019-09-10
 * Email: hieupham@bitmark.com
 * Copyright © 2019 Bitmark. All rights reserved.
 */
class BitmarkSdkHttpObserver @Inject constructor() : HttpObserver {

    override fun onRequest(request: Request?) {
        Tracer.INFO.log("BitmarkSdk", request.toString())
    }

    override fun onRespond(response: Response?) {
        Tracer.INFO.log("BitmarkSdk", response.toString())
    }
}