/**
 * SPDX-License-Identifier: ISC
 * Copyright © 2014-2019 Bitmark. All rights reserved.
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 */
package com.bitmark.registry.util.livedata

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import io.reactivex.*
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import javax.inject.Inject

class RxLiveDataTransformer @Inject constructor() {

    companion object {
        private const val TAG = "RxLiveDataTransformer"
    }

    private val compositeDisposable = CompositeDisposable()

    fun dispose() {
        compositeDisposable.dispose()
    }

    fun <T> maybe(source: Maybe<T>): LiveData<Resource<T>> {
        val result = MutableLiveData<Resource<T>>()
        subscribe(
            source.observeOn(AndroidSchedulers.mainThread()).doOnSubscribe {
                result.value = Resource.loading(null)
            }.subscribe({ data ->
                result.value = Resource.success(data)
            }, { throwable ->
                result.value = Resource.error(throwable, null)
            }, {
                result.value = Resource.success(null)
            })
        )
        return result
    }

    fun <T> single(source: Single<T>): LiveData<Resource<T>> {
        val result = MutableLiveData<Resource<T>>()
        subscribe(
            source.observeOn(AndroidSchedulers.mainThread()).doOnSubscribe {
                result.value = Resource.loading(null)
            }.subscribe { data, throwable ->
                if (null != throwable) {
                    result.value = Resource.error(throwable, null)
                } else {
                    result.value = Resource.success(data)
                }
            })
        return result
    }

    fun completable(source: Completable): LiveData<Resource<Any>> {
        val result = MutableLiveData<Resource<Any>>()
        subscribe(
            source.observeOn(AndroidSchedulers.mainThread()).doOnSubscribe {
                result.value = Resource.loading(null)
            }.subscribe({
                result.value = Resource.success(null)
            }, { throwable ->
                result.value = Resource.error(throwable, null)
            })
        )
        return result
    }

    fun <T> observable(source: Observable<T>): LiveData<Resource<T>> {
        val result = MutableLiveData<Resource<T>>()
        subscribe(
            source.observeOn(AndroidSchedulers.mainThread()).doOnSubscribe {
                result.value = Resource.loading(null)
            }.subscribe({ data ->
                result.value = Resource.success(data)
            }, { throwable ->
                result.value = Resource.error(throwable, null)
            }, {
                result.value = Resource.success(null)
            })
        )
        return result
    }

    fun <T> flowable(source: Flowable<T>): LiveData<Resource<T>> {
        val result = MutableLiveData<Resource<T>>()
        subscribe(
            source.observeOn(AndroidSchedulers.mainThread()).doOnSubscribe {
                result.value = Resource.loading(null)
            }.subscribe({ data ->
                result.value = Resource.success(data)
            }, { throwable ->
                result.value = Resource.error(throwable, null)
            }, {
                result.value = Resource.success(null)
            })
        )
        return result
    }

    private fun subscribe(disposable: Disposable) {
        compositeDisposable.add(disposable)
    }
}