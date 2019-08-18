package com.bitmark.registry.data.source.remote.api.service

import io.reactivex.Completable
import io.reactivex.Single
import okhttp3.MultipartBody
import okhttp3.RequestBody
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
        @Header("identifier") identifier: String, @Path("assetId") assetId: String, @Path(
            "sender"
        ) sender: String, @Query("receiver") receiver: String
    ): Single<Response<ResponseBody>>

    @DELETE("v2/files/{assetId}/{sender}")
    fun deleteAssetFile(
        @Path("assetId") assetId: String, @Path("sender") sender: String, @Query(
            "receiver"
        ) receiver: String
    ): Completable

    @Multipart
    @POST("v2/files/{assetId}/{sender}")
    fun uploadAssetFile(
        @Path("assetId") assetId: String, @Path("sender") sender: String, @Part(
            "data_key_alg"
        ) keyAlgorithm: RequestBody, @Part("enc_data_key") encKey: RequestBody, @Part(
            "orig_content_type"
        ) orgContentType: RequestBody, @Part("access") access: RequestBody, @Part file: MultipartBody.Part
    ): Completable

    @HEAD("v2/files/{assetId}/{sender}")
    fun checkExistingAssetFile(
        @Path("assetId") assetId: String, @Path("sender") sender: String
    ): Single<Response<Void>>

    @Multipart
    @PUT("v2/access/{assetId}/{sender}")
    fun grantAccessAssetFile(
        @Path("assetId") assetId: String, @Path("sender") sender: String, @Part(
            "access"
        ) access: RequestBody
    ): Completable

    @GET("v2/files")
    fun getDownloadableAssets(@Query("receiver") receiver: String): Single<Map<String, Array<String>>>
}