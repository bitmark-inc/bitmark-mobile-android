/**
 * SPDX-License-Identifier: ISC
 * Copyright © 2014-2019 Bitmark. All rights reserved.
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 */
package com.bitmark.registry.feature.properties

import androidx.lifecycle.Lifecycle
import com.bitmark.registry.data.source.AccountRepository
import com.bitmark.registry.data.source.BitmarkRepository
import com.bitmark.registry.feature.BaseViewModel
import com.bitmark.registry.feature.realtime.RealtimeBus
import com.bitmark.registry.util.livedata.CompositeLiveData
import com.bitmark.registry.util.livedata.RxLiveDataTransformer

class PropertiesViewModel(
    lifecycle: Lifecycle,
    private val accountRepo: AccountRepository,
    private val bitmarkRepo: BitmarkRepository,
    private val rxLiveDataTransformer: RxLiveDataTransformer,
    private val realtimeBus: RealtimeBus
) : BaseViewModel(lifecycle) {

    private val getBitmarkCountLiveData = CompositeLiveData<Long>()

    override fun onCreate() {
        super.onCreate()
        realtimeBus.bitmarkDeletedPublisher.subscribe(this) { getBitmarkCount() }
        realtimeBus.bitmarkSavedPublisher.subscribe(this) { getBitmarkCount() }
    }

    override fun onDestroy() {
        realtimeBus.unsubscribe(this)
        rxLiveDataTransformer.dispose()
        super.onDestroy()
    }

    internal fun getBitmarkCountLiveData() =
        getBitmarkCountLiveData.asLiveData()

    internal fun getBitmarkCount() =
        getBitmarkCountLiveData.add(rxLiveDataTransformer.single(accountRepo.getAccountNumber().flatMap { accountNumber ->
            bitmarkRepo.countUsableBitmarks(
                accountNumber
            )
        }))
}