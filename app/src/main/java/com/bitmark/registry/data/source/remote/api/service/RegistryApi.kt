package com.bitmark.registry.data.source.remote.api.service

import com.bitmark.registry.data.source.remote.api.response.AssetClaimingInfoResponse
import io.reactivex.Single
import retrofit2.http.GET
import retrofit2.http.Query


/**
 * @author Hieu Pham
 * @since 2019-08-09
 * Email: hieupham@bitmark.com
 * Copyright © 2019 Bitmark. All rights reserved.
 */
interface RegistryApi {

    @GET("asset-for-claim")
    fun getAssetClaimingInfo(@Query("asset_id") assetId: String): Single<AssetClaimingInfoResponse>
}