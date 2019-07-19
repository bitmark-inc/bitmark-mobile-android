package com.bitmark.registry.data.source.remote.api.service

import io.reactivex.Single
import retrofit2.http.GET
import retrofit2.http.Path


/**
 * @author Hieu Pham
 * @since 2019-07-18
 * Email: hieupham@bitmark.com
 * Copyright © 2019 Bitmark. All rights reserved.
 */
interface KeyAccountServerApi {

    @GET("/{accountNumber}")
    fun getEncPubKey(@Path("accountNumber") accountNumber: String): Single<Map<String, String>>
}