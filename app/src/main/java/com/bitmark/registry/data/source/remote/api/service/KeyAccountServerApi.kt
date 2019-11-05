/**
 * SPDX-License-Identifier: ISC
 * Copyright © 2014-2019 Bitmark. All rights reserved.
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 */
package com.bitmark.registry.data.source.remote.api.service

import io.reactivex.Single
import retrofit2.http.GET
import retrofit2.http.Path

interface KeyAccountServerApi {

    @GET("/{accountNumber}")
    fun getEncPubKey(@Path("accountNumber") accountNumber: String): Single<Map<String, String>>
}