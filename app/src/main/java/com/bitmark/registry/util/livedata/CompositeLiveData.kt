/**
 * SPDX-License-Identifier: ISC
 * Copyright © 2014-2019 Bitmark. All rights reserved.
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 */
package com.bitmark.registry.util.livedata

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData

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