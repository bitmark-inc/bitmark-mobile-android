package com.bitmark.registry.data.source.remote

import com.bitmark.registry.data.source.remote.api.converter.Converter
import com.bitmark.registry.data.source.remote.api.middleware.RxErrorHandlingComposer
import com.bitmark.registry.data.source.remote.api.request.RegisterEncKeyRequest
import com.bitmark.registry.data.source.remote.api.request.RegisterJwtRequest
import com.bitmark.registry.data.source.remote.api.service.*
import io.intercom.android.sdk.Intercom
import io.intercom.android.sdk.identity.Registration
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
    coreApi: CoreApi,
    mobileServerApi: MobileServerApi,
    fileCourierServerApi: FileCourierServerApi,
    keyAccountServerApi: KeyAccountServerApi,
    registryApi: RegistryApi,
    converter: Converter,
    rxErrorHandlingComposer: RxErrorHandlingComposer
) : RemoteDataSource(
    coreApi,
    mobileServerApi,
    fileCourierServerApi,
    keyAccountServerApi,
    registryApi,
    converter,
    rxErrorHandlingComposer
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

    fun getEncPubKey(accountNumber: String): Single<String> =
        keyAccountServerApi.getEncPubKey(accountNumber).subscribeOn(
            Schedulers.io()
        ).map { res -> res["encryption_pubkey"] }

    fun registerIntercomUser(id: String) = Completable.fromAction {
        val registration = Registration.create().withUserId(id)
        Intercom.client().registerIdentifiedUser(registration)
    }.subscribeOn(Schedulers.io())
}