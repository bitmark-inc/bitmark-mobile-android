package com.bitmark.registry.data.ext

import androidx.room.EmptyResultSetException
import com.bitmark.registry.data.source.remote.api.error.HttpException
import com.bitmark.registry.data.source.remote.api.error.NetworkException
import com.bitmark.registry.data.source.remote.api.error.UnknownException
import java.io.IOException


/**
 * @author Hieu Pham
 * @since 2019-08-21
 * Email: hieupham@bitmark.com
 * Copyright © 2019 Bitmark. All rights reserved.
 */

fun Throwable.isNetworkError() = this is IOException

fun Throwable.isDbRecNotFoundError() = this is EmptyResultSetException

fun Throwable.isHttpError() =
    this is com.bitmark.apiservice.utils.error.HttpException || this is retrofit2.HttpException

fun Throwable.toRemoteError() = when {
    isNetworkError() -> NetworkException(this)
    isHttpError() -> {
        val code =
            (this as? com.bitmark.apiservice.utils.error.HttpException)?.statusCode
                ?: (this as? retrofit2.HttpException)?.code() ?: -1
        HttpException(code)
    }
    else -> UnknownException(this)
}