package com.bitmark.registry.data.source.remote.api.middleware

import com.bitmark.registry.data.source.ext.toRemoteError
import io.reactivex.*
import io.reactivex.functions.Function
import retrofit2.Call
import retrofit2.CallAdapter
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import java.lang.reflect.Type

/**
 * @author Hieu Pham
 * @since 7/1/19
 * Email: hieupham@bitmark.com
 * Copyright © 2019 Bitmark. All rights reserved.
 */
class RxErrorHandlingCallAdapterFactory : CallAdapter.Factory() {

    private val instance = RxJava2CallAdapterFactory.createAsync()

    override fun get(
        returnType: Type,
        annotations: Array<Annotation>,
        retrofit: Retrofit
    ): CallAdapter<*, *>? = RxCallAdapterWrapper(
        instance.get(
            returnType,
            annotations,
            retrofit
        ) as CallAdapter<Any, Any>
    )

    class RxCallAdapterWrapper<R>(private val wrapped: CallAdapter<R, Any>) :
        CallAdapter<R, Any> {

        override fun responseType(): Type = wrapped.responseType()

        override fun adapt(call: Call<R>): Any {
            return when (val result = wrapped.adapt(call)) {
                is Single<*> -> {
                    result.onErrorResumeNext { e ->
                        Single.error(e.toRemoteError())
                    }
                }

                is Observable<*> -> {
                    result.onErrorResumeNext(Function { e ->
                        Observable.error(e.toRemoteError())
                    })
                }

                is Completable -> {
                    result.onErrorResumeNext { e ->
                        Completable.error(e.toRemoteError())
                    }
                }

                is Flowable<*> -> {
                    result.onErrorResumeNext(Function { e ->
                        Flowable.error(e.toRemoteError())
                    })
                }

                is Maybe<*> -> {
                    result.onErrorResumeNext(Function { e ->
                        Maybe.error(e.toRemoteError())
                    })
                }

                else -> result
            }
        }
    }
}