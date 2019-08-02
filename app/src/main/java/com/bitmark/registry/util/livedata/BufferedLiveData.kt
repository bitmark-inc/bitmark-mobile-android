package com.bitmark.registry.util.livedata

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.MutableLiveData
import java.util.*


/**
 * @author Hieu Pham
 * @since 2019-08-02
 * Email: hieupham@bitmark.com
 * Copyright © 2019 Bitmark. All rights reserved.
 */
class BufferedLiveData<T>(private val lifecycle: Lifecycle) :
    MutableLiveData<T>(), LifecycleObserver {

    private val buffer = LinkedList<T>()

    init {
        lifecycle.addObserver(this)
    }

    override fun onActive() {
        if (buffer.isEmpty()) {
            super.onActive()
        } else {
            while (buffer.isNotEmpty()) {
                setValue(buffer.pop())
            }
        }
    }

    override fun setValue(value: T) {
        if (lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
            super.setValue(value)
        } else {
            buffer.add(value)
        }
    }
}