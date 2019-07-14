package com.bitmark.registry.feature.main.properties

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
    private val bitmarkRepo: BitmarkRepository,
    private val rxLiveDataTransformer: RxLiveDataTransformer,
    private val realtimeBus: RealtimeBus
) : BaseViewModel() {

    private val getBitmarkCountLiveData = CompositeLiveData<Long>()

    override fun onCreate() {
        super.onCreate()
        realtimeBus.bitmarkDeletedPublisher.subscribe(this) { getBitmarkCount() }
        realtimeBus.bitmarkInsertedPublisher.subscribe(this) { getBitmarkCount() }
    }

    override fun onDestroy() {
        realtimeBus.unsubscribe(this)
        rxLiveDataTransformer.dispose()
        super.onDestroy()
    }

    internal fun getBitmarkCountLiveData() =
        getBitmarkCountLiveData.asLiveData()

    internal fun getBitmarkCount() =
        getBitmarkCountLiveData.add(rxLiveDataTransformer.single(bitmarkRepo.countStoredBitmark()))
}