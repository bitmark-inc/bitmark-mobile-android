package com.bitmark.registry.feature.properties

import androidx.lifecycle.Lifecycle
import com.bitmark.registry.data.source.AccountRepository
import com.bitmark.registry.data.source.BitmarkRepository
import com.bitmark.registry.feature.BaseViewModel
import com.bitmark.registry.feature.realtime.RealtimeBus
import com.bitmark.registry.util.livedata.CompositeLiveData
import com.bitmark.registry.util.livedata.RxLiveDataTransformer


/**
 * @author Hieu Pham
 * @since 2019-07-14
 * Email: hieupham@bitmark.com
 * Copyright © 2019 Bitmark. All rights reserved.
 */
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
        getBitmarkCountLiveData.add(rxLiveDataTransformer.single(accountRepo.getAccountInfo().map { a -> a.first }.flatMap { accountNumber ->
            bitmarkRepo.countUsableBitmarks(
                accountNumber
            )
        }))
}