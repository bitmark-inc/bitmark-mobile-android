/**
 * SPDX-License-Identifier: ISC
 * Copyright © 2014-2019 Bitmark. All rights reserved.
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 */
package com.bitmark.registry.data.source.remote.api.service

import com.bitmark.registry.data.source.remote.api.request.RegisterDeviceTokenRequest
import com.bitmark.registry.data.source.remote.api.request.RegisterJwtRequest
import com.bitmark.registry.data.source.remote.api.response.AssetClaimingRequestsResponse
import io.reactivex.Completable
import io.reactivex.Single
import retrofit2.http.*

interface MobileServerApi {

    @POST("api/auth")
    fun registerJwt(@Body request: RegisterJwtRequest): Single<Map<String, String>>

    @POST("api/accounts")
    fun registerAccount(): Completable

    @DELETE("api/push_uuids/{device_token}")
    fun deleteDeviceToken(@Path("device_token") deviceToken: String): Completable

    @POST("api/push_uuids")
    fun registerDeviceToken(
        @Header("requester") requester: String,
        @Header("timestamp") timestamp: String,
        @Header("signature") signature: String,
        @Body request: RegisterDeviceTokenRequest
    ): Completable

    @GET("api/claim_requests")
    fun getAssetClaimRequests(@Query("asset_id") assetId: String): Single<AssetClaimingRequestsResponse>
}