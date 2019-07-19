package com.bitmark.registry.data.source.remote.api.service

import io.reactivex.Completable
import io.reactivex.Single
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.*


/**
 * @author Hieu Pham
 * @since 7/1/19
 * Email: hieupham@bitmark.com
 * Copyright © 2019 Bitmark. All rights reserved.
 */
interface FileCourierServerApi {

    @Streaming
    @GET("v2/files/{assetId}/{sender}")
    fun downloadAssetFile(
        @Path("assetId") assetId: String, @Path("sender") sender: String, @Query(
            "receiver"
        ) receiver: String
    ): Single<Response<ResponseBody>>

    @DELETE("v2/files/{assetId}/{sender}")
    fun deleteAssetFile(
        @Path("assetId") assetId: String, @Path("sender") sender: String, @Query(
            "receiver"
        ) receiver: String
    ): Completable
}