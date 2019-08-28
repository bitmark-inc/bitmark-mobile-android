package com.bitmark.registry.data.source

import com.bitmark.registry.data.source.local.AppLocalDataSource
import com.bitmark.registry.data.source.remote.AppRemoteDataSource
import com.bitmark.registry.data.source.remote.api.error.HttpException
import com.bitmark.registry.data.source.remote.api.middleware.Cache
import io.reactivex.Completable
import io.reactivex.schedulers.Schedulers


/**
 * @author Hieu Pham
 * @since 2019-07-25
 * Email: hieupham@bitmark.com
 * Copyright © 2019 Bitmark. All rights reserved.
 */
class AppRepository(
    private val localDataSource: AppLocalDataSource,
    private val remoteDataSource: AppRemoteDataSource
) : AbsRepository() {

    fun deleteQrCodeFile() =
        localDataSource.deleteQrCodeFile().onErrorResumeNext { e ->
            if (e is IllegalArgumentException) Completable.complete() else Completable.error(
                e
            )
        }

    fun deleteSharePref() = localDataSource.deleteSharePref()

    fun deleteDatabase() = localDataSource.deleteDatabase()

    fun registerDeviceToken(
        requester: String,
        timestamp: String,
        signature: String,
        token: String,
        intercomId: String?
    ) = remoteDataSource.registerDeviceToken(
        requester,
        timestamp,
        signature,
        token,
        intercomId
    )

    fun deleteDeviceToken(deviceToken: String) =
        remoteDataSource.deleteDeviceToken(deviceToken).onErrorResumeNext { e ->
            if (e is HttpException && e.code == 404) {
                Completable.complete()
            } else {
                Completable.error(e)
            }
        }

    fun deleteCache() =
        Completable.fromAction { Cache.getInstance().clear() }.subscribeOn(
            Schedulers.io()
        )

    fun deleteFiles(path: String) = localDataSource.deleteFiles(path)
}