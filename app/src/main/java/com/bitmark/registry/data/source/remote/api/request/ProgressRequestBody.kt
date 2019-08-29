package com.bitmark.registry.data.source.remote.api.request

import com.bitmark.registry.BuildConfig
import okhttp3.MediaType
import okhttp3.RequestBody
import okio.*
import java.io.IOException


/**
 * @author Hieu Pham
 * @since 2019-08-29
 * Email: hieupham@bitmark.com
 * Copyright © 2019 Bitmark. All rights reserved.
 */
class ProgressRequestBody(
    private val delegate: RequestBody,
    private val listener: ProgressListener
) : RequestBody() {

    private var ignored = false

    override fun contentType(): MediaType? = delegate.contentType()

    override fun contentLength(): Long = try {
        delegate.contentLength()
    } catch (e: IOException) {
        -1
    }

    override fun writeTo(sink: BufferedSink) {
        if (BuildConfig.DEBUG && !ignored) {
            // workaround for dealing with OkHttp Logging interceptor
            // this method is call twice since logging interceptor need to load this for logging
            // so ignore the first time call
            ignored = true
            return
        }
        val countingSink = CountingSink(sink, contentLength())
        val bufferSink = countingSink.buffer()
        delegate.writeTo(bufferSink)
        bufferSink.flush()
    }

    inner class CountingSink(delegate: Sink, private val contentLength: Long) :
        ForwardingSink(delegate) {

        private var bytesWritten = 0L

        override fun write(source: Buffer, byteCount: Long) {
            super.write(source, byteCount)
            bytesWritten += byteCount
            listener.update(
                bytesWritten,
                contentLength,
                bytesWritten >= contentLength
            )
        }
    }

}

interface ProgressListener {
    fun update(bytesRead: Long, contentLength: Long, done: Boolean)
}