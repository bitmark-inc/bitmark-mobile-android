package com.bitmark.registry.data.source.local.api

import android.content.Context
import io.reactivex.*
import io.reactivex.schedulers.Schedulers
import javax.inject.Inject


/**
 * @author Hieu Pham
 * @since 7/2/19
 * Email: hieupham@bitmark.com
 * Copyright © 2019 Bitmark. All rights reserved.
 */
class FileStorageApi @Inject constructor(context: Context) {

    private val fileStorageGateway = FileStorageGateway(context)

    fun <T> rxSingle(action: (FileStorageGateway) -> T): Single<T> {
        return Single.create(SingleOnSubscribe<T> { emt ->
            try {
                emt.onSuccess(action.invoke(fileStorageGateway))
            } catch (e: Throwable) {
                emt.onError(e)
            }
        }).subscribeOn(Schedulers.io())
    }

    fun rxCompletable(action: (FileStorageGateway) -> Unit): Completable {
        return Completable.create { e ->
            action.invoke(fileStorageGateway)
            e.onComplete()
        }.subscribeOn(Schedulers.io())
    }

    fun <T> rxMaybe(action: (FileStorageGateway) -> T): Maybe<T> {
        return Maybe.create(MaybeOnSubscribe<T> { emt ->
            try {
                emt.onSuccess(action.invoke(fileStorageGateway))
                emt.onComplete()
            } catch (e: Throwable) {
                emt.onError(e)
            }
        }).subscribeOn(Schedulers.io())
    }

    fun filesDir() = fileStorageGateway.filesDir()
}