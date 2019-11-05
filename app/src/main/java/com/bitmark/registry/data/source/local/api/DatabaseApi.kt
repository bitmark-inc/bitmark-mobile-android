/**
 * SPDX-License-Identifier: ISC
 * Copyright © 2014-2019 Bitmark. All rights reserved.
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 */
package com.bitmark.registry.data.source.local.api

import io.reactivex.Completable
import io.reactivex.Maybe
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers

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