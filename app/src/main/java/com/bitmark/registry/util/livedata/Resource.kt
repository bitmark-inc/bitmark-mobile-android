/**
 * SPDX-License-Identifier: ISC
 * Copyright © 2014-2019 Bitmark. All rights reserved.
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 */
package com.bitmark.registry.util.livedata

class Resource<T>(
    private val status: Int, private val data: T?,
    private val throwable: Throwable?
) {

    companion object {
        const val SUCCESS = 0x01
        const val LOADING = 0x02
        const val ERROR = 0x03

        fun <T> success(data: T?): Resource<T> = Resource(SUCCESS, data, null)

        fun <T> loading(data: T?): Resource<T> = Resource(LOADING, data, null)

        fun <T> error(throwable: Throwable, data: T?) =
            Resource(ERROR, data, throwable)
    }

    fun data() = data

    fun throwable() = throwable

    fun isError() = status == ERROR

    fun isSuccess() = status == SUCCESS

    fun isLoading() = status == LOADING


}