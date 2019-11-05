/**
 * SPDX-License-Identifier: ISC
 * Copyright © 2014-2019 Bitmark. All rights reserved.
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 */
package com.bitmark.registry.data.source.remote.api.middleware

import com.bitmark.apiservice.middleware.HttpObserver
import com.bitmark.registry.logging.Tracer
import okhttp3.Request
import okhttp3.Response
import javax.inject.Inject

class BitmarkSdkHttpObserver @Inject constructor() : HttpObserver {

    override fun onRequest(request: Request?) {
        Tracer.INFO.log("BitmarkSdk", request.toString())
    }

    override fun onRespond(response: Response?) {
        Tracer.INFO.log("BitmarkSdk", response.toString())
    }
}