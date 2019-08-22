package com.bitmark.registry.util.extension

import androidx.room.EmptyResultSetException
import java.io.IOException


/**
 * @author Hieu Pham
 * @since 2019-08-21
 * Email: hieupham@bitmark.com
 * Copyright © 2019 Bitmark. All rights reserved.
 */

fun Throwable.isNetworkError() = this is IOException

fun Throwable.isDbRecNotFoundError() = this is EmptyResultSetException