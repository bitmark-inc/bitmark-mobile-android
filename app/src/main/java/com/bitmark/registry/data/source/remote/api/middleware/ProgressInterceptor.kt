/**
 * SPDX-License-Identifier: ISC
 * Copyright © 2014-2019 Bitmark. All rights reserved.
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 */
package com.bitmark.registry.data.source.remote.api.middleware

import com.bitmark.registry.data.source.remote.api.response.ProgressListener
import com.bitmark.registry.data.source.remote.api.response.ProgressResponseBody
import io.reactivex.subjects.PublishSubject
import okhttp3.Response
import javax.inject.Inject

class ProgressInterceptor @Inject constructor(
    private val publisher: PublishSubject<Progress>
) :
    Interceptor() {

    override fun getTag(): String? = null

    override fun intercept(chain: okhttp3.Interceptor.Chain): Response {
        val identifier = chain.request().header("identifier") ?: ""
        val response = super.intercept(chain)
        return response.newBuilder().body(
            ProgressResponseBody(
                response.body!!,
                object : ProgressListener {
                    override fun update(
                        bytesRead: Long,
                        contentLength: Long,
                        done: Boolean
                    ) {

                        if (contentLength > 0) {
                            publisher.onNext(
                                Progress(
                                    identifier,
                                    (bytesRead * 100 / contentLength).toInt(),
                                    bytesRead >= contentLength
                                )
                            )
                        } else if (done) {
                            publisher.onNext(Progress(identifier, 100, true))
                        }

                    }

                })
        ).build()
    }
}
