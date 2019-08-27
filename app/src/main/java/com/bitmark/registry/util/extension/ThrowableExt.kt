package com.bitmark.registry.util.extension

import com.bitmark.registry.data.source.remote.api.error.HttpException
import com.bitmark.registry.data.source.remote.api.error.NetworkException


/**
 * @author Hieu Pham
 * @since 2019-08-21
 * Email: hieupham@bitmark.com
 * Copyright © 2019 Bitmark. All rights reserved.
 */

fun Throwable.isNetworkError() = this is NetworkException

fun Throwable.isHttpError() = this is HttpException