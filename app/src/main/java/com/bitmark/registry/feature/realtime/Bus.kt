package com.bitmark.registry.feature.realtime

import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.subjects.PublishSubject
import kotlin.reflect.KClass


/**
 * @author Hieu Pham
 * @since 2019-07-26
 * Email: hieupham@bitmark.com
 * Copyright © 2019 Bitmark. All rights reserved.
 */
abstract class Bus {

    private val observerMap = mutableMapOf<KClass<*>, MutableList<Disposable>>()

    fun <H : Any> unsubscribe(host: H) {
        val observers = observerMap[host::class]
        if (observers.isNullOrEmpty()) return
        observers.forEach { o -> o.dispose() }
        observerMap.remove(host::class)
    }

    inner class Publisher<T>(internal val publisher: PublishSubject<T>) {

        fun <H : Any> subscribe(host: H, consumer: (T) -> Unit) {
            val disposable = publisher.observeOn(AndroidSchedulers.mainThread())
                .subscribe(consumer)
            val kclass = host::class
            if (observerMap[kclass] == null) {
                observerMap[kclass] = mutableListOf()
            }
            observerMap[kclass]?.add(disposable)
        }
    }
}