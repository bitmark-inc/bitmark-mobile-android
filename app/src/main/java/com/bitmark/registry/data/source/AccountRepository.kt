package com.bitmark.registry.data.source

import com.bitmark.registry.data.source.local.AccountLocalDataSource
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

    fun getAccountInfo(): Single<Pair<String, Boolean>> {
        return localDataSource.getAccountInfo()
    }

    fun saveAccountInfo(
        accountNumber: String,
        authRequired: Boolean
    ): Single<Pair<String, Boolean>> {
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
}