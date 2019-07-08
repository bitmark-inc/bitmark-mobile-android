package com.bitmark.registry.util.livedata

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData


/**
 * @author Hieu Pham
 * @since 7/4/19
 * Email: hieupham@bitmark.com
 * Copyright © 2019 Bitmark. All rights reserved.
 */
class CompositeLiveData<T> {

    private val mediatorLiveData = MediatorLiveData<Resource<T>>()

    fun asLiveData(): LiveData<Resource<T>> = mediatorLiveData

    fun add(source: LiveData<Resource<T>>) {
        mediatorLiveData.addSource(source) { r -> mediatorLiveData.value = r }
    }

    fun remove(source: LiveData<Resource<T>>) {
        mediatorLiveData.removeSource(source)
    }
}