package com.bitmark.registry.feature.account

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.MutableLiveData
import com.bitmark.registry.data.source.AccountRepository
import com.bitmark.registry.feature.BaseViewModel
import com.bitmark.registry.feature.realtime.RealtimeBus
import com.bitmark.registry.util.extension.set
import com.bitmark.registry.util.livedata.BufferedLiveData
import com.bitmark.registry.util.livedata.CompositeLiveData
import com.bitmark.registry.util.livedata.RxLiveDataTransformer
import io.reactivex.android.schedulers.AndroidSchedulers


/**
 * @author Hieu Pham
 * @since 2019-07-22
 * Email: hieupham@bitmark.com
 * Copyright © 2019 Bitmark. All rights reserved.
 */
class SettingsViewModel(
    lifecycle: Lifecycle,
    private val accountRepo: AccountRepository,
    private val rxLiveDataTransformer: RxLiveDataTransformer,
    private val realtimeBus: RealtimeBus
) :
    BaseViewModel(lifecycle) {

    private val getAccountNumberLiveData = CompositeLiveData<String>()

    internal val checkCloudServiceRequiredLiveData =
        BufferedLiveData<Boolean>(lifecycle)

    internal val cloudServiceRequiredChangedLiveData =
        MutableLiveData<Boolean>()

    internal fun getAccountNumberLiveData() =
        getAccountNumberLiveData.asLiveData()

    internal fun getAccountNumber() {
        getAccountNumberLiveData.add(rxLiveDataTransformer.single(accountRepo.getAccountInfo().map { p -> p.first }))
    }

    internal fun checkCloudServiceRequired() = subscribe(
        accountRepo.checkCloudServiceRequired().observeOn(AndroidSchedulers.mainThread()).subscribe { required, e ->
            checkCloudServiceRequiredLiveData.setValue(required || e != null)
        })

    override fun onStart() {
        super.onStart()
        realtimeBus.cloudServiceRequiredChangedPublisher.subscribe(this) { required ->
            cloudServiceRequiredChangedLiveData.set(required)
        }
    }

    override fun onDestroy() {
        realtimeBus.unsubscribe(this)
        rxLiveDataTransformer.dispose()
        super.onDestroy()
    }
}