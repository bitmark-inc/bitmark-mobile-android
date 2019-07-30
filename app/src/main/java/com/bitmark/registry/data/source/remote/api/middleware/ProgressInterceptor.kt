package com.bitmark.registry.data.source.remote.api.middleware

import com.bitmark.registry.data.source.remote.api.response.ProgressListener
import com.bitmark.registry.data.source.remote.api.response.ProgressResponseBody
import io.reactivex.subjects.PublishSubject
import okhttp3.Response


/**
 * @author Hieu Pham
 * @since 2019-07-29
 * Email: hieupham@bitmark.com
 * Copyright © 2019 Bitmark. All rights reserved.
 */
class ProgressInterceptor(private val publisher: PublishSubject<Progress>) :
    Interceptor() {

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
                        publisher.onNext(
                            Progress(
                                identifier,
                                (bytesRead * 100 / contentLength).toInt(),
                                bytesRead >= contentLength
                            )
                        )
                    }

                })
        ).build()
    }
}
