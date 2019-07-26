package com.bitmark.registry.data.source.remote.api.service

import com.bitmark.registry.data.source.remote.api.request.RegisterJwtRequest
import io.reactivex.Completable
import io.reactivex.Single
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.POST
import retrofit2.http.Path


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

    @DELETE("api/push_uuids/{device_token}")
    fun deleteDeviceToken(@Path("device_token") deviceToken: String): Completable

}