package com.bitmark.registry.data.source.local.api

import android.content.Context
import io.reactivex.Completable
import io.reactivex.Single
import io.reactivex.SingleOnSubscribe
import io.reactivex.schedulers.Schedulers
import javax.inject.Inject


/**
 * @author Hieu Pham
 * @since 7/2/19
 * Email: hieupham@bitmark.com
 * Copyright © 2019 Bitmark. All rights reserved.
 */
class FileApi @Inject constructor(private val context: Context) {

    private val fileStorageGateway = FileStorageGateway(context)

    fun <T> rxSingle(action: (FileStorageGateway) -> T): Single<T> {
        return Single.create(SingleOnSubscribe<T> { e ->
            try {
                e.onSuccess(action.invoke(fileStorageGateway))
            } catch (ex: Exception) {
                e.onError(ex)
            }
        }).subscribeOn(Schedulers.io())
    }

    fun rxCompletable(action: (FileStorageGateway) -> Unit): Completable {
        return Completable.create { e ->
            action.invoke(fileStorageGateway)
            e.onComplete()
        }.subscribeOn(Schedulers.io())
    }
}