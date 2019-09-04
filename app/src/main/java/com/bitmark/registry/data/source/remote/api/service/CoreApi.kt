package com.bitmark.registry.data.source.remote.api.service

import com.bitmark.registry.data.source.remote.api.request.RegisterEncKeyRequest
import io.reactivex.Completable
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Path


/**
 * @author Hieu Pham
 * @since 7/1/19
 * Email: hieupham@bitmark.com
 * Copyright © 2019 Bitmark. All rights reserved.
 */
interface CoreApi {

    @POST("v2/encryption_keys/{accountNumber}")
    fun registerEncryptionKey(
        @Path("accountNumber") accountNumber: String, @Body
        request: RegisterEncKeyRequest
    ): Completable
}