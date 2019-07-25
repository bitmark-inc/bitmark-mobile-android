package com.bitmark.registry.data.source

import com.bitmark.cryptography.crypto.encoder.Hex.HEX
import com.bitmark.registry.data.model.ActionRequired
import com.bitmark.registry.data.source.local.AccountLocalDataSource
import com.bitmark.registry.data.source.local.ActionRequiredDeletedListener
import com.bitmark.registry.data.source.remote.AccountRemoteDataSource
import com.bitmark.registry.data.source.remote.api.middleware.Cache
import io.reactivex.Completable
import io.reactivex.Single


/**
 * @author Hieu Pham
 * @since 7/2/19
 * Email: hieupham@bitmark.com
 * Copyright © 2019 Bitmark. All rights reserved.
 */
class AccountRepository(
    private val localDataSource: AccountLocalDataSource,
    private val remoteDataSource: AccountRemoteDataSource
) : AbsRepository() {

    fun setActionRequiredDeletedListener(listener: ActionRequiredDeletedListener) {
        localDataSource.setActionRequiredDeletedListener(listener)
    }

    fun getAccountInfo(): Single<Pair<String, Boolean>> {
        return localDataSource.getAccountInfo()
    }

    fun saveAccountInfo(
        accountNumber: String,
        authRequired: Boolean
    ): Completable {
        return localDataSource.saveAccountInfo(accountNumber, authRequired)
    }

    fun registerMobileServerJwt(
        timestamp: String,
        signature: String,
        requester: String
    ): Single<String> {
        return remoteDataSource.registerMobileServerJwt(
            timestamp,
            signature,
            requester
        ).doAfterSuccess { jwt -> Cache.getInstance().mobileServerJwt = jwt }
    }

    fun registerMobileServerAccount(
        timestamp: String,
        signature: String,
        requester: String
    ): Completable {
        return registerMobileServerJwt(
            timestamp,
            signature,
            requester
        ).flatMapCompletable { remoteDataSource.registerMobileServerAccount() }
    }

    fun registerEncPubKey(
        accountNumber: String,
        encPubKey: String,
        signature: String
    ): Completable {
        return remoteDataSource.registerEncPubKey(
            accountNumber,
            encPubKey,
            signature
        )
    }

    fun getEncPubKey(accountNumber: String): Single<ByteArray> =
        localDataSource.getEncPubKey(accountNumber).onErrorResumeNext {
            remoteDataSource.getEncPubKey(accountNumber).flatMap { key ->
                localDataSource.saveEncPubKey(
                    accountNumber,
                    key
                ).andThen(Single.just(key))
            }.map { key -> HEX.decode(key) }
        }

    fun getActionRequired(): Single<List<ActionRequired>> =
        localDataSource.getActionRequired()

    fun addActionRequired(actions: List<ActionRequired>) =
        localDataSource.addActionRequired(actions)

    fun deleteActionRequired(actionId: ActionRequired.Id) =
        localDataSource.deleteActionRequired(actionId)

}