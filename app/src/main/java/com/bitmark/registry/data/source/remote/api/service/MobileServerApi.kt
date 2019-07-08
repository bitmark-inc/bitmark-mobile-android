package com.bitmark.registry.data.source.remote.api.service

import com.bitmark.registry.data.source.remote.api.request.RegisterJwtRequest
import io.reactivex.Completable
import io.reactivex.Single
import retrofit2.http.Body
import retrofit2.http.POST


/**
 * @author Hieu Pham
 * @since 7/1/19
 * Email: hieupham@bitmark.com
 * Copyright © 2019 Bitmark. All rights reserved.
 */
interface MobileServerApi {

    @POST("api/auth")
    fun registerJwt(@Body request: RegisterJwtRequest): Single<Map<String, String>>

    @POST("api/accounts")
    fun registerAccount(): Completable
}