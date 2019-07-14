package com.bitmark.registry.data.source.local.api

import io.reactivex.Completable
import io.reactivex.Maybe
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers


/**
 * @author Hieu Pham
 * @since 2019-07-10
 * Email: hieupham@bitmark.com
 * Copyright © 2019 Bitmark. All rights reserved.
 */
class DatabaseApi(private val databaseGateway: DatabaseGateway) {

    fun <T> rxMaybe(func: (DatabaseGateway) -> Maybe<T>): Maybe<T> {
        return func.invoke(databaseGateway).subscribeOn(Schedulers.io())
    }

    fun <T> rxSingle(func: (DatabaseGateway) -> Single<T>): Single<T> {
        return func.invoke(databaseGateway).subscribeOn(Schedulers.io())
    }

    fun rxCompletable(func: (DatabaseGateway) -> Completable): Completable {
        return func.invoke(databaseGateway).subscribeOn(Schedulers.io())
    }
}