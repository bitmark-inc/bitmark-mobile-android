/**
 * SPDX-License-Identifier: ISC
 * Copyright © 2014-2019 Bitmark. All rights reserved.
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 */
package com.bitmark.registry.data.source.remote.api.service

import com.bitmark.registry.data.source.remote.api.request.RegisterEncKeyRequest
import io.reactivex.Completable
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Path

interface CoreApi {

    @POST("v2/encryption_keys/{accountNumber}")
    fun registerEncryptionKey(
        @Path("accountNumber") accountNumber: String, @Body
        request: RegisterEncKeyRequest
    ): Completable
}