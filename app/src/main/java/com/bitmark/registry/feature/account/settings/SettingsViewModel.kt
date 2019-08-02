package com.bitmark.registry.feature.account.settings

import androidx.lifecycle.Lifecycle
import com.bitmark.registry.data.source.AccountRepository
import com.bitmark.registry.feature.BaseViewModel
import com.bitmark.registry.util.livedata.CompositeLiveData
import com.bitmark.registry.util.livedata.RxLiveDataTransformer


/**
 * @author Hieu Pham
 * @since 2019-07-22
 * Email: hieupham@bitmark.com
 * Copyright © 2019 Bitmark. All rights reserved.
 */
class SettingsViewModel(
    lifecycle: Lifecycle,
    private val accountRepo: AccountRepository,
    private val rxLiveDataTransformer: RxLiveDataTransformer
) :
    BaseViewModel(lifecycle) {

    private val getAccountNumberLiveData = CompositeLiveData<String>()

    internal fun getAccountNumberLiveData() =
        getAccountNumberLiveData.asLiveData()

    internal fun getAccountNumber() {
        getAccountNumberLiveData.add(rxLiveDataTransformer.single(accountRepo.getAccountInfo().map { p -> p.first }))
    }

    override fun onDestroy() {
        rxLiveDataTransformer.dispose()
        super.onDestroy()
    }
}