package com.bitmark.registry.data.source.remote.api.middleware

import com.bitmark.registry.data.source.ext.toRemoteError
import io.reactivex.*
import io.reactivex.functions.Function
import javax.inject.Inject


/**
 * @author Hieu Pham
 * @since 2019-08-27
 * Email: hieupham@bitmark.com
 * Copyright © 2019 Bitmark. All rights reserved.
 */
class RxErrorHandlingComposer @Inject constructor() {

    fun <T> single(publisher: SingleOnSubscribe<T>): Single<T> =
        Single.create(publisher).onErrorResumeNext { e -> Single.error(e.toRemoteError()) }

    fun completable(publisher: CompletableOnSubscribe) =
        Completable.create(publisher).onErrorResumeNext { e ->
            Completable.error(
                e.toRemoteError()
            )
        }

    fun <T> maybe(publisher: MaybeOnSubscribe<T>) =
        Maybe.create(publisher).onErrorResumeNext(
            Function { e -> Maybe.error(e.toRemoteError()) })

    fun <T> observable(publisher: ObservableOnSubscribe<T>) =
        Observable.create(publisher).onErrorResumeNext(Function { e ->
            Observable.error(
                e.toRemoteError()
            )
        })

    fun <T> flowable(
        publisher: FlowableOnSubscribe<T>,
        backpressureStrategy: BackpressureStrategy
    ) =
        Flowable.create(publisher, backpressureStrategy).onErrorResumeNext(
            Function { e -> Flowable.error(e.toRemoteError()) })


}