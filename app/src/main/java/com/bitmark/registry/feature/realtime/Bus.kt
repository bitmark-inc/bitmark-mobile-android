/**
 * SPDX-License-Identifier: ISC
 * Copyright © 2014-2019 Bitmark. All rights reserved.
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 */
package com.bitmark.registry.feature.realtime

import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.subjects.PublishSubject
import kotlin.reflect.KClass

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