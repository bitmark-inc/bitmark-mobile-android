package com.bitmark.registry.data.source.remote

import com.bitmark.registry.data.source.remote.api.converter.Converter
import com.bitmark.registry.data.source.remote.api.request.RegisterEncKeyRequest
import com.bitmark.registry.data.source.remote.api.request.RegisterJwtRequest
import com.bitmark.registry.data.source.remote.api.service.CoreApi
import com.bitmark.registry.data.source.remote.api.service.FileCourierServerApi
import com.bitmark.registry.data.source.remote.api.service.MobileServerApi
import io.reactivex.Completable
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import javax.inject.Inject


/**
 * @author Hieu Pham
 * @since 7/2/19
 * Email: hieupham@bitmark.com
 * Copyright © 2019 Bitmark. All rights reserved.
 */
class AccountRemoteDataSource @Inject constructor(
    coreApi: CoreApi, mobileServerApi: MobileServerApi,
    fileCourierServerApi: FileCourierServerApi, converter: Converter
) : RemoteDataSource(
    coreApi, mobileServerApi, fileCourierServerApi, converter
) {

    fun registerMobileServerJwt(
        timestamp: String,
        signature: String,
        requester: String
    ): Single<String> {
        return mobileServerApi.registerJwt(
            RegisterJwtRequest(
                timestamp,
                signature,
                requester
            )
        ).map { response ->
            response.getValue("jwt_token")
        }.subscribeOn(Schedulers.io())
    }

    fun registerMobileServerAccount(): Completable {
        return mobileServerApi.registerAccount().subscribeOn(Schedulers.io())
    }

    fun registerEncPubKey(
        accountNumber: String,
        encPubKey: String,
        signature: String
    ): Completable {
        return coreApi.registerEncryptionKey(
            accountNumber,
            RegisterEncKeyRequest(encPubKey, signature)
        ).subscribeOn(Schedulers.io())
    }
}